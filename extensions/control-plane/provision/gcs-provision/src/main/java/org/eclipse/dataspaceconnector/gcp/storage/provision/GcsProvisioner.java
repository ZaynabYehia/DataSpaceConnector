/*
 *  Copyright (c) 2022 Google LLC
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Google LCC - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.gcp.storage.provision;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.eclipse.dataspaceconnector.gcp.core.common.GcpCredential;
import org.eclipse.dataspaceconnector.gcp.core.common.GcpException;
import org.eclipse.dataspaceconnector.gcp.core.common.GcpServiceAccount;
import org.eclipse.dataspaceconnector.gcp.core.iam.IamService;
import org.eclipse.dataspaceconnector.gcp.core.iam.IamServiceImpl;
import org.eclipse.dataspaceconnector.gcp.core.storage.StorageServiceImpl;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.response.StatusResult;
import org.eclipse.dataspaceconnector.spi.transfer.provision.Provisioner;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DeprovisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionResponse;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.completedFuture;

public class GcsProvisioner implements Provisioner<GcsResourceDefinition, GcsProvisionedResource> {

    private final Monitor monitor;
    private GcpCredential gcpCredential;

    public GcsProvisioner(Monitor monitor, GcpCredential gcpCredential) {
        this.monitor = monitor;
        this.gcpCredential = gcpCredential;
    }

    @Override
    public boolean canProvision(ResourceDefinition resourceDefinition) {
        return resourceDefinition instanceof GcsResourceDefinition;
    }

    @Override
    public boolean canDeprovision(ProvisionedResource resourceDefinition) {
        return resourceDefinition instanceof GcsProvisionedResource;
    }

    @Override
    public CompletableFuture<StatusResult<ProvisionResponse>> provision(
            GcsResourceDefinition resourceDefinition, Policy policy) {
        var bucketName = resourceDefinition.getId();
        var bucketLocation = resourceDefinition.getLocation();
        var projectId = resourceDefinition.getProjectId();
        var dataAddress = resourceDefinition.getDataAddress();


        monitor.debug("GCS Bucket request submitted: " + bucketName);

        var resourceName = resourceDefinition.getId() + "-bucket";
        var processId = resourceDefinition.getTransferProcessId();
        try {
            var googleCredentials = gcpCredential.resolveGoogleCredential(dataAddress);
            var storageClient = createDefaultStorageClient(googleCredentials);
            var storageService = new StorageServiceImpl(storageClient, monitor);
            var bucket = storageService.getOrCreateEmptyBucket(bucketName, bucketLocation);
            if (!storageService.isEmpty(bucketName)) {
                return completedFuture(StatusResult.failure(ResponseStatus.FATAL_ERROR, String.format("Bucket: %s already exists and is not empty.", bucketName)));
            }
            var iamService = IamServiceImpl.Builder.newInstance(monitor, projectId, googleCredentials).build();
            var serviceAccount = createServiceAccount(processId, bucketName, projectId, iamService);
            storageService.addProviderPermissions(bucket, serviceAccount);
            var token = iamService.createAccessToken(serviceAccount);

            var resource = getProvisionedResource(resourceDefinition, resourceName, bucketName, serviceAccount);

            var response = ProvisionResponse.Builder.newInstance().resource(resource).secretToken(token).build();
            return CompletableFuture.completedFuture(StatusResult.success(response));
        } catch (GcpException e) {
            return completedFuture(StatusResult.failure(ResponseStatus.FATAL_ERROR, e.toString()));
        }
    }

    @Override
    public CompletableFuture<StatusResult<DeprovisionedResource>> deprovision(
            GcsProvisionedResource provisionedResource, Policy policy) {
        try {
            var googleCredentials = gcpCredential.resolveGoogleCredential(provisionedResource.getDataAddress());
            var iamService = IamServiceImpl.Builder.newInstance(monitor, provisionedResource.getProjectId(), googleCredentials).build();
            iamService.deleteServiceAccountIfExists(
                    new GcpServiceAccount(provisionedResource.getServiceAccountEmail(),
                            provisionedResource.getServiceAccountName(), ""));
        } catch (GcpException e) {
            return completedFuture(StatusResult.failure(ResponseStatus.FATAL_ERROR,
                    String.format("Deprovision failed with: %s", e.getMessage())));
        }
        return CompletableFuture.completedFuture(StatusResult.success(
                DeprovisionedResource.Builder.newInstance()
                        .provisionedResourceId(provisionedResource.getId()).build()));
    }

    private GcpServiceAccount createServiceAccount(String processId, String buckedName, String projectId, IamService iamService) {
        var serviceAccountName = sanitizeServiceAccountName(processId);
        var uniqueServiceAccountDescription = generateUniqueServiceAccountDescription(processId, buckedName);
        return iamService.getOrCreateServiceAccount(serviceAccountName, uniqueServiceAccountDescription);
    }

    @NotNull
    private String sanitizeServiceAccountName(String processId) {
        // service account ID must be between 6 and 30 characters and can contain lowercase alphanumeric characters and dashes
        String processIdWithoutConstantChars = processId.replace("-", "");
        var maxAllowedSubstringLength = Math.min(26, processIdWithoutConstantChars.length());
        var uniqueId = processIdWithoutConstantChars.substring(0, maxAllowedSubstringLength);
        return "edc-" + uniqueId;
    }

    @NotNull
    private String generateUniqueServiceAccountDescription(String transferProcessId, String bucketName) {
        return String.format("transferProcess:%s\nbucket:%s", transferProcessId, bucketName);
    }

    private GcsProvisionedResource getProvisionedResource(GcsResourceDefinition resourceDefinition, String resourceName, String bucketName, GcpServiceAccount serviceAccount) {
        return GcsProvisionedResource.Builder.newInstance()
                .id(resourceDefinition.getId())
                .resourceDefinitionId(resourceDefinition.getId())
                .location(resourceDefinition.getLocation())
                .projectId(resourceDefinition.getProjectId())
                .storageClass(resourceDefinition.getStorageClass())
                .serviceAccountEmail(serviceAccount.getEmail())
                .serviceAccountName(serviceAccount.getName())
                .transferProcessId(resourceDefinition.getTransferProcessId())
                .resourceName(resourceName)
                .bucketName(bucketName)
                .hasToken(true).build();
    }

    /**
     * Creates {@link Storage} for the specified project using application default credentials
     *
     * @param googleCredentials
     * @return {@link Storage}
     */
    private Storage createDefaultStorageClient(GoogleCredentials googleCredentials) {
        return StorageOptions.newBuilder()
                .setCredentials(googleCredentials).build().getService();
    }

    private IamService createIamService(Monitor monitor, String projectId, GoogleCredentials googleCredentials) {
        return  IamServiceImpl.Builder.newInstance(monitor, projectId, googleCredentials).build();
    }
}
