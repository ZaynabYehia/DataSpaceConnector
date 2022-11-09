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

import org.eclipse.dataspaceconnector.gcp.core.common.GcpCredentials;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Inject;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionManager;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ResourceManifestGenerator;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;


public class GcsProvisionExtension implements ServiceExtension {

    @Inject
    private ProvisionManager provisionManager;

    @Inject
    private ResourceManifestGenerator manifestGenerator;

    @Override
    public String name() {
        return "GCP storage provisioner";
    }

    @Inject
    private Vault vault;

    @Inject
    private TypeManager typeManager;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();
        var gcpCredential = new GcpCredentials(vault, typeManager, monitor);

        var provisioner = new GcsProvisioner(monitor, gcpCredential);
        provisionManager.register(provisioner);

        manifestGenerator.registerGenerator(new GcsConsumerResourceDefinitionGenerator());
    }



}