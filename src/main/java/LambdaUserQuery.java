import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;

public class LambdaUserQuery implements RequestHandler<Map<String, Object>, String> {
    
    // Sets the name of the output s3 bucket
    private static final String bucket = "output125";
    // Dates are format MM/dd/yyyy, we will use the Date class to process them
    private static final SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");
    // Defines SNS topic ARN 
    private static final String sns_topic_arn = "arn:aws:sns:us-east-1:767397988710:user-notification";

    @Override
    public String handleRequest(Map<String, Object> input, Context context) {
        // Extracts user input parameters
        String country = (String) input.get("country");
        String start_date = (String) input.get("start-date");
        String end_date = (String) input.get("end-date");
        
        // Retrieves sales data from the rifhgt file (file with name "Sales data/sales_data_Country.csv")
        AmazonS3 s3Client = AmazonS3ClientBuilder.standard().build();

        String key = "Sales data/sales_data_" + country + ".csv";
        S3Object s3Object = s3Client.getObject(bucket, key);
        S3ObjectInputStream inputStream = s3Object.getObjectContent();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;

        // Processes and filters data based on user input parameters
        // For each line, checks if the sales data is within the range of the input start and end dates
        List<String> filteredData = new ArrayList<>();
        try {
            reader.readLine();// Skips header
            while ((line = reader.readLine()) != null) {
                // System.out.println("Line processed : " + line); // Used for logging
                String[] values = line.split(";");
                String invoice_date = values[0];
                try {
                    // Conversion of date to Date type to compare them
                    Date invoiceDate = formatter.parse(invoice_date); 
                    Date startDate = formatter.parse(start_date);   
                    Date endDate = formatter.parse(end_date); 

                    if (invoiceDate.after(startDate) && invoiceDate.before(endDate)) {
                        filteredData.add(line); // If the date is between the two input dates, it adds the line to the list
                        // System.out.println("Line added : " + line); // Used for logging
                    }
                   
                } catch (java.text.ParseException e) {
                    e.printStackTrace();
                }
            }
            // Generates the output file 

            /* Sets the output key
                Output format is "User queries/sales_data_Country_MMddyyyy_MMddyyyy.csv"
                This ensures a unique ID for each user request since the ID is made of the 3 parameters.
            */
            String start_month = start_date.split("/")[0];
            String start_day = start_date.split("/")[1];
            String start_year = start_date.split("/")[2];
            String end_month = end_date.split("/")[0];
            String end_day = end_date.split("/")[1];
            String end_year = end_date.split("/")[2];
            String unformatted_start_date = start_month + start_day + start_year;
            String unformatted_end_date = end_month + end_day + end_year;   
            
            String outputKey = "User queries/sales_data_" + country + "_" + unformatted_start_date + "_" + unformatted_end_date + ".csv"; 
            // System.out.println("output key : " + outputKey); // Used for logging

            String outputContent = "InvoiceDate;StockCode;TotalQuantity;TotalAmount\n";
            for (String record : filteredData) {
                outputContent += record + "\n";
            }
            // Uploads the file to the output S3 bucket
            byte[] contentBytes = outputContent.toString().getBytes();
            ByteArrayInputStream inputStreamForOutput = new ByteArrayInputStream(contentBytes);
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(contentBytes.length);

            s3Client.putObject(bucket, outputKey, inputStreamForOutput, metadata);

            // Sends a SNS notification
            String message = "Your query has been succesfully processed. You can find it at " + outputKey + " in the output S3 bucket.";
            SnsClient snsClient = SnsClient.builder().region(Region.US_EAST_1).build();
            PublishRequest request = PublishRequest.builder().message(message).topicArn(sns_topic_arn).build();
            PublishResponse snsResponse = snsClient.publish(request);
            System.out.println(snsResponse.messageId() + " Message sent. Status is " + snsResponse.sdkHttpResponse().statusCode());

        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Query processed successfully";
    }
}