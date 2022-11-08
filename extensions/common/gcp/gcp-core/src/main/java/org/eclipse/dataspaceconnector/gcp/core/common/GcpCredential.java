package org.eclipse.dataspaceconnector.gcp.core.common;

import com.fasterxml.jackson.core.io.JsonEOFException;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import org.eclipse.dataspaceconnector.gcp.core.storage.GcsStoreSchema;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

public class GcpCredential {

    private final Base64.Decoder b64Decoder;
    private final Vault vault;
    private final TypeManager typeManager;
    private final Monitor monitor;

    public GcpCredential(Vault vault, TypeManager typeManager, Monitor monitor) {
        this.vault = vault;
        this.typeManager = typeManager;
        this.b64Decoder = Base64.getDecoder();
        this.monitor = monitor;
    }

    /**
     * Returns the Google Credentials which will be created based on the following order:
     * if keyName is provided in the dataAddress
     * then Google Credentials should be retrieved from a token which is persisted in the vault
     * if ACCESS_TOKEN_VALUE is provided in the dataAddress
     * then Google Credentials should be retrieved from a token which is provided in the ACCESS_TOKEN_VALUE in b64 format
     * if SERVICE_ACCOUNT_KEY_NAME is provided in the dataAddress
     * then Google Credentials should be retrieved from a Credentials file which is persisted in the vault
     * if SERVICE_ACCOUNT_VALUE is provided in the dataAddress
     * then Google Credentials should be retrieved from a Credentials file which is provided in the SERVICE_ACCOUNT_VALUE in b64 format
     * otherwise it will be created based on the Application Default Credentials
     *
     * @return GoogleCredentials
     */
    public GoogleCredentials resolveGoogleCredential(DataAddress dataAddress) {
        //1- try to retrieve GoogleCredentials from a token
        GoogleCredentials googleCredentials = resolveGcpAccessToken(dataAddress);

        //2- try to retrieve GoogleCredentials from the credential file
        if (googleCredentials == null)
            googleCredentials = resolveGcpCredentialFile(dataAddress);

        //3- try to retrieve GoogleCredentials from the default Credentials
        if (googleCredentials == null)
            googleCredentials = createGoogleCredential();

        return googleCredentials;
    }


    private GoogleCredentials resolveGcpCredentialFile(DataAddress dataAddress) {
        var googleAccessKeyContent = "";

        if (dataAddress.getProperty(GcsStoreSchema.SERVICE_ACCOUNT_KEY_NAME) != null && !dataAddress.getProperty(GcsStoreSchema.SERVICE_ACCOUNT_KEY_NAME).isEmpty()) {
            googleAccessKeyContent = vault.resolveSecret(dataAddress.getProperty(GcsStoreSchema.SERVICE_ACCOUNT_KEY_NAME));
        } else if (dataAddress.getProperty(GcsStoreSchema.SERVICE_ACCOUNT_VALUE) != null && !dataAddress.getProperty(GcsStoreSchema.SERVICE_ACCOUNT_VALUE).isEmpty()) {
            try {
                googleAccessKeyContent = new String(b64Decoder.decode(dataAddress.getProperty(GcsStoreSchema.SERVICE_ACCOUNT_VALUE)));
                if(!googleAccessKeyContent.contains("service_account"))
                    throw new GcpException("SERVICE_ACCOUNT_VALUE is not provided as a valid service account key file.");
            } catch (IllegalArgumentException ex) {
                throw new GcpException("SERVICE_ACCOUNT_VALUE is not provided in a valid base64 format.");
            }
        }

        if (!googleAccessKeyContent.isEmpty()) {
            return createGoogleCredential(googleAccessKeyContent);
        } else
            return null;
    }

    private GoogleCredentials resolveGcpAccessToken(DataAddress dataAddress) {

        var tokenContent = "";

        if (dataAddress.getKeyName() != null && !dataAddress.getKeyName().isEmpty())
            tokenContent = vault.resolveSecret(dataAddress.getKeyName());
        else if (dataAddress.getProperty(GcsStoreSchema.ACCESS_TOKEN_KEY_NAME) != null && !dataAddress.getProperty(GcsStoreSchema.ACCESS_TOKEN_KEY_NAME).isEmpty())
            tokenContent = vault.resolveSecret(dataAddress.getProperty(GcsStoreSchema.ACCESS_TOKEN_KEY_NAME));
        else if (dataAddress.getProperty(GcsStoreSchema.ACCESS_TOKEN_VALUE) != null && !dataAddress.getProperty(GcsStoreSchema.ACCESS_TOKEN_VALUE).isEmpty()) {
            try {
                tokenContent = new String(b64Decoder.decode(dataAddress.getProperty(GcsStoreSchema.ACCESS_TOKEN_VALUE)));
            } catch (IllegalArgumentException ex) {
                throw new GcpException("ACCESS_TOKEN_VALUE is not provided in a valid base64 format.");
            }
        }

        if (!tokenContent.isEmpty()) {
            GcpAccessToken gcsAccessToken;
            try {
                gcsAccessToken = typeManager.readValue(tokenContent, GcpAccessToken.class);
            } catch (EdcException ex) {
                throw new GcpException("ACCESS_TOKEN is not in a valid GcpAccessToken format.");
            }
            return createGoogleCredential(gcsAccessToken);
        } else {
            return null;
        }
    }


    /**
     * Returns the credentials instance from the given access token.
     *
     * @param gcpAccessToken
     * @return GoogleCredentials
     */
    public GoogleCredentials createGoogleCredential(GcpAccessToken gcpAccessToken) {
        try {
            monitor.info("Gcp: The provided token will be used to resolve the google credentials.");
            return GoogleCredentials.create(
                    new AccessToken(gcpAccessToken.getToken(),
                            new Date(gcpAccessToken.getExpiration())));
        } catch (Exception e) {
            throw new GcpException("Error while getting the default credentials.", e);
        }
    }

    /**
     * Returns credentials from a JSON stream, which contains a Service Account key file.
     */
    public GoogleCredentials createGoogleCredential(String serviceAccountKey) {
        try {
            monitor.info("Gcp: The provided credentials file will be used to resolve the google credentials.");
            return GoogleCredentials.fromStream(new ByteArrayInputStream(serviceAccountKey.getBytes(StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new GcpException("Error while getting the credentials from the credentials file.", e);
        }
    }

    /**
     * Returns the Application Default Credentials which are used to identify and authorize the whole application. The following are searched (in order) to find the Application Default Credentials:
     * Credentials file pointed to by the GOOGLE_APPLICATION_CREDENTIALS environment variable
     * Credentials provided by the Google Cloud SDK gcloud auth application-default login command
     * Google App Engine built-in credentials
     * Google Cloud Shell built-in credentials
     * Google Compute Engine built-in credentials
     *
     * @return GoogleCredentials
     */
    public GoogleCredentials createGoogleCredential() {
        try {
            monitor.info("Gcp: The default Credentials will be used to resolve the google credentials.");
            return GoogleCredentials.getApplicationDefault();
        } catch (IOException e) {
            throw new GcpException("Error while getting the default credentials.", e);
        }
    }
}
