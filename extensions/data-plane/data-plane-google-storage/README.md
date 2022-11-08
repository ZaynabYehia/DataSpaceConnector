## Google cloud storage data plane module

### About this module

This module contains a Data Plane extension to copy data to and from Google cloud storage.

### Authentication

Currently, Google storage data plane supports three different approaches for authentication:
* Default authentication:
  * authenticates against the Google Cloud API using the [Application Default Credentials](https://cloud.google.com/docs/authentication#adc).
  * These will automatically be provided if the connector is deployed with the correct service account attached.
* Service Account key file
  * SERVICE_ACCOUNT_KEY_NAME 
  * SERVICE_ACCOUNT_VALUE 
* Access token:
  * ACCESS_TOKEN_KEY_NAME 
  * ACCESS_TOKEN_VALUE

### Data source properties

| Key               | Description                                                                                                    | Mandatory |
|:------------------|:---------------------------------------------------------------------------------------------------------------|---|
| type | GoogleCloudStorage                                                                                             | X |
| bucket_name | A valid name of your bucket                                                                                    | X |
| blob_name | Name of your blob/object in the bucket. Currently only a single blob name                                      | X |
| service_account_key_name | It should reference a vault entry containing a [Service Account Key File](https://docs.microsoft.com) in json. |  |
| service_account_value | It should contain a [Service Account Key File](https://docs.microsoft.com) in json                             |  |
| access_token_key_name | It should contain an access vault entry containing an [Access Token](https://docs.microsoft.com).              |  |
| access_token_value | It should contain an [Access Token](https://docs.microsoft.com).                                               |  |

### Data destination properties

| Key               | Description                                                                                                    | Mandatory with provisioner | Mandatory without provisioner |
|:------------------|:---------------------------------------------------------------------------------------------------------------|----------------------------|---------------------------|
| type | GoogleCloudStorage                                                                                             | X                          | X                         |
| bucket_name | A valid name of your bucket                                                                                    |                           | X                         |
| blob_name | Name of your blob/object in the bucket. The source blob name will be used if it is not provided!               |                           |                           |
| storage_class | STANDARD/ NEARLINE/ COLDLINE/ ARCHIVE / [More info](https://cloud.google.com/storage/docs/storage-classes)     | X                          |                           |
| location | [Available regions](https://cloud.google.com/storage/docs/locations#location-r)                                | X                          |                           |
| service_account_key_name | It should reference a vault entry containing a [Service Account Key File](https://docs.microsoft.com) in json. |  |
| service_account_value | It should contain a [Service Account Key File](https://docs.microsoft.com) in json                             |  |
| access_token_key_name | It should contain an access vault entry containing an [Access Token](https://docs.microsoft.com).              |  |
| access_token_value | It should contain an [Access Token](https://docs.microsoft.com).                                               |  |
