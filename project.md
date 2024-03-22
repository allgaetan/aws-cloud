### Gaétan ALLAIRE, Cloud and Edge infrastructures Project

# Proposed architecture

## Data storage

Data storage is ensured with S3 buckets. S3 buckets provide highly durable and scalable object storage for storing transactional data files and processed results. S3 allows for seamless integration with other AWS services and offers configurable access controls.

### Input data

The original CSV dataset "online-retail.csv" will be uploaded in its own public S3 bucket (input bucket).

### Output data

To ensure an easier setup and analysis of the outputs, every output will be put in the same S3 bucket (output bucket), with different folders :
- Files splitted by country/ : for the CSV country files
- Sales data/ : for the country files with calculated amount and sales per product per day
- User queries/ : for the output files from the second component

## Data computing

Data computing is done in every step with Lambda functions. Lambda functions are event-driven, allowing for automatic scaling and cost-effective execution based on demand.

### LambdaSplitter 

The first Lambda function is triggered by the upload of data in the input bucket. The source code reads and maps the the data from the entire CSV file according to countries and creates separate files with data from each country. Finally, it uploads the files to the output bucket with a prefix "Files splitted by country/" which creates a specific folder in the output bucket.

### LambdaCalculator

The second Lambda function is triggered by the upload of data in the output bucket. The source code ensures that the data read is from the "Files splitted by country/" folder, and maps the data from each country by invoice date and stock code, and returns files with data about the sales per product per day. These files are uploaded to the output bucket with a prefix "Sales data/sales_data_" so the data is stored in another folder.

### LambdaUserQuery 

The third Lambda function takes arguments from the user through a JSON test event : a start date, an end date and a country name. The source code retrieves the arguments, searches for the right sales data file from the "Sales data/" folder, and filters the records for which the invoice date is between the two input dates. Finally, a file is generated and uploaded to the output bucket with a prefix "User queries/" so the data is stored in another folder. The user is also notified by email using a SNS topic and SNS subscription.

## Data communication

Data is passed directly through S3 events into the Lambda functions, and is uploaded directly with S3 putObject() method. 
I haven't had the time to try it, but communication could be implemented with SQS messages between Lambda functions and S3 buckets to ensure more reliable message delivery and processing. However, SQS supports a maximum message size of 256kb, and considering the amount of data that is being processed in this situation, it could cause limitations.

When the ouput file from the user query for the second component is uploaded, a notification is sent to a SNS topic, and the user receives a notification by email ensured by a subscription to this topic.
