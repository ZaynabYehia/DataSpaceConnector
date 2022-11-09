## Google cloud storage data plane module

### About this module

This module contains a Data Plane extension to copy data to and from Google cloud storage.

### Authentication

Google storage data plane supports three different approaches for authentication:
* Default authentication:
  * Authenticates against the Google Cloud API using the [Application Default Credentials](https://cloud.google.com/docs/authentication#adc).
  * These will automatically be provided if the connector is deployed with the correct service account attached.
* Service Account key file which could be provided as a Vault entry or the encoded in b64 in the data address properties
  


### Data source properties

| Key               | Description                                                                                                                                                                                                                                                                                                                                                            | Mandatory |
|:------------------|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---|
| type | GoogleCloudStorage                                                                                                                                                                                                                                                                                                                                                     | X |
| bucket_name | A valid name of your bucket                                                                                                                                                                                                                                                                                                                                            | X |
| blob_name | Name of your blob/object in the bucket. Currently only a single blob name                                                                                                                                                                                                                                                                                              | X |
| service_account_key_name | It should reference a vault entry containing a [Service Account Key File](https://cloud.google.com/iam/docs/creating-managing-service-account-keys#creating) in json.<br/>Note: when you don't want to use the provisioner, you need to have the vault entry in both consumer and provider vaults. Or you can use the service_account_value in the data address properties. |  |
| service_account_value | It should contain a [Service Account Key File](https://cloud.google.com/iam/docs/creating-managing-service-account-keys#creating) in json encoded with base64                                                                                                                                                                                                          |  |

### Data destination properties

| Key               | Description                                                                                                    | Mandatory with provisioner | Mandatory without provisioner |
|:------------------|:---------------------------------------------------------------------------------------------------------------|----------------------------|---------------------------|
| type | GoogleCloudStorage                                                                                             | X                          | X                         |
| bucket_name | A valid name of your bucket                                                                                    |                           | X                         |
| blob_name | Name of your blob/object in the bucket. The source blob name will be used if it is not provided!               |                           |                           |
| storage_class | STANDARD/ NEARLINE/ COLDLINE/ ARCHIVE / [More info](https://cloud.google.com/storage/docs/storage-classes)     | X                          |                           |
| location | [Available regions](https://cloud.google.com/storage/docs/locations#location-r)                                | X                          |                           |
| service_account_key_name | It should reference a vault entry containing a [Service Account Key File](https://cloud.google.com/iam/docs/creating-managing-service-account-keys#creating) in json.<br/>Note: when you don't want to use the provisioner, you need to have the vault entry in both consumer and provider vaults. Or you can use the service_account_value in the data address properties. |  |
| service_account_value | It should contain a [Service Account Key File](https://cloud.google.com/iam/docs/creating-managing-service-account-keys#creating) in json encoded with base64                                                                                                                                                                                                          |  |
