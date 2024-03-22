import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;

import java.io.*;
import java.util.*;

public class LambdaSplitter implements RequestHandler<S3Event, String> {

    // Sets the name of the input and output s3 buckets
    private static final String input_bucket = "dataset456";
    private static final String output_bucket = "output125";

    @Override
    public String handleRequest(S3Event event, Context context) {

        AmazonS3 s3Client = AmazonS3ClientBuilder.standard().build();

        for (S3EventNotification.S3EventNotificationRecord record : event.getRecords()) {

            String key = record.getS3().getObject().getUrlDecodedKey();
            S3Object s3Object = s3Client.getObject(input_bucket, key);
            S3ObjectInputStream inputStream = s3Object.getObjectContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;

            Map<String, StringWriter> countryWriters = new HashMap<>(); // Creates a map with each country and a specific StringWriter associated
            
            try {
                // Reads each line and add it using the correct StringWriter for the country
                reader.readLine(); // Skips the header
                while ((line = reader.readLine()) != null) {
                    String[] values = line.split(";");
                    String country = values[7]; 
                    StringWriter writer = countryWriters.computeIfAbsent(country, k -> new StringWriter());
                    writer.write(line + "\n");
                }
                // Uploads the new csv country files to the output bucket
                for (Map.Entry<String, StringWriter> entry : countryWriters.entrySet()) {

                    String country = entry.getKey();
                    String outputKey = "Files splitted by country/" + country + ".csv";
                    String content = entry.getValue().toString();

                    byte[] contentBytes = content.getBytes();
                    ByteArrayInputStream inputStreamForOutput = new ByteArrayInputStream(contentBytes);
                    ObjectMetadata metadata = new ObjectMetadata();
                    metadata.setContentLength(contentBytes.length);

                    s3Client.putObject(output_bucket, outputKey, inputStreamForOutput, metadata);
                }
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return "Data splitted correctly";
    }
}