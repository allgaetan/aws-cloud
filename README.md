### Gaétan ALLAIRE, Cloud and Edge infrastructures Project

# Configuration 

## S3 Buckets

There are 2 S3 buckets named "dataset456" and "output125".
They have the same configuration which is the following :

- Choose "ACLs enabled"
- Uncheck "Block all public access" 
- Enable "Bucket versioning"
- Create bucket

In the bucket policy, add for dataset456 : 

```
{
    "Version": "2012-10-17",
    "Id": "Policy1710164923835",
    "Statement": [
        {
            "Sid": "Stmt1710164913819",
            "Effect": "Allow",
            "Principal": "*",
            "Action": "s3:*",
            "Resource": "arn:aws:s3:::dataset456/*"
        }
    ]
}
```

And add for output125 :

```
{
    "Version": "2012-10-17",
    "Id": "Policy1710164923835",
    "Statement": [
        {
            "Sid": "Stmt1710164913819",
            "Effect": "Allow",
            "Principal": "*",
            "Action": "s3:*",
            "Resource": "arn:aws:s3:::output125/*"
        }
    ]
}
```

## Lambda functions 

> **_NOTE:_** You might need to adjust the timeout for each lambda function, the initial 15 seconds were too short for me.

### LambdaSplitter configuration 

#### In the function creation pannel 

- Function name : LambdaSplitter 
- Runtime : Java 21 
- Execution role : "Use an existing role" and then "LabRole"
- Finally click "Create function"

#### In the function pannel

- Code source : "Upload from" then ".zip or .jar file" and select aws-cloud-1.0-SNAPSHOT.jar
- Runtime settings : Click "Edit" then in the "Handler" part, enter the class name (LambdaSplitter, it already implements a handler interface so there is no need to add ::Handler)
- Function overview : "Add trigger" then select "S3" as source, then "dataset456" as bucket, event type "All object create events" and check the acknowledge part. Finally select "Add"

### LambdaCalculator configuration 

#### In the function creation pannel 

- Function name : LambdaCalculator 
- Runtime : Java 21
- Execution role : "Use an existing role" and then "LabRole"
- Finally click "Create function"

#### In the function pannel 

- Code source : "Upload from" then ".zip or .jar file" and select aws-cloud-1.0-SNAPSHOT.jar after packaging the project.
- Runtime settings : Click "Edit" then in the "Handler" part, enter the class name (LambdaCalculator, it already implements a handler interface so there is no need to add ::Handler)
- Function overview : "Add trigger" then select "S3" as source, then "output125" as bucket, event type "All object create events", "Files splitted by country/" as prefix and check the acknowledge part. Finally select "Add"

### LambdaUserQuery configuration

#### In the function creation pannel 

- Function name : LambdaUserQuery 
- Runtime : Java 21 
- Execution role : "Use an existing role" and then "LabRole"
- Finally click "Create function"

#### In the function pannel 

- Code source : "Upload from" then ".zip or .jar file" and select aws-cloud-1.0-SNAPSHOT.jar after packaging the project.
- Runtime settings : Click "Edit" then in the "Handler" part, enter the class name (LambdaUserQuery, it already implements a handler interface so there is no need to add ::Handler)

## SNS Topic and subscriptions

### In the SNS Topic pannel 

- Create Topic 
- Choose "Standard"
- Name : "user-notification"
- Click "Create topic"

### In the subscriptions pannel 

- Create Subscription
- Topic ARN : select the ARN of the topic precedently created
- Protocol : choose "EMAIL"
- Endpoint : enter your email address, 
- Click "Create subscription"
You will need to access your email account to confirm the subscription. 

# Run the project

Package the project using Maven with :
```
mvn package
```
Then in each Lambda function, in "Code source", click "Upload from" then ".zip or .jar file" and select aws-cloud-1.0-SNAPSHOT.jar which should be in the "target" folder of the project.

## First component

Go to the "dataset456" S3 bucket, upload "online-retail.csv" and grant it public-read access.
When the two first Lambda functions are done computing, you should find in the "output125" bucket two folders.
- Files splitted by country/ : this folder contains the original CSV file splitted by country
- Sales data/ : this folder contains the sales data for each country with InvoiceDate, StockCode, TotalQuantity, and TotalAmount

## Second component

To test the second component, create and launch a JSON test event with this model :

```
{
    "start-date":"MM/dd/yyyy",
    "end-date":"MM/dd/yyyy",
    "country":"Country Name"
}
```

When LambdaUserQuery is done computing, you should see a new folder in the "output125" bucket "User queries/" which contains the output file from your test event. The format of the file should be "User queries/sales_data_Country_MMddyyyy_to_MMddyyyy.csv". You should also receive a notification on the email that you used to set the SNS subscription.
