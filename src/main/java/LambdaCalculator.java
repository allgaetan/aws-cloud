import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;

import java.io.*;
import java.util.*;

public class LambdaCalculator implements RequestHandler<S3Event, String> {

    // Sets the name of the output s3 bucket
    private static final String bucket = "output125";

    @Override
    public String handleRequest(S3Event event, Context context) {

        AmazonS3 s3Client = AmazonS3ClientBuilder.standard().build();

        for (S3EventNotification.S3EventNotificationRecord record : event.getRecords()) {

            String full_path = record.getS3().getObject().getUrlDecodedKey(); // full_path looks like "folder/country.csv"
            if (!full_path.startsWith("Files splitted by country/")) {
                continue; // Skips processing if the key doesn't match the expected prefix
            }

            String key = full_path.split("/")[1];
            S3Object s3Object = s3Client.getObject(bucket, full_path);
            S3ObjectInputStream inputStream = s3Object.getObjectContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;

            Map<String, Map<String, Integer>> salesData = new HashMap<>(); // Creates a map to map stock code and quantity (quantities mapped by stock code) by invoice date
            Map<String, Double> priceData = new HashMap<>(); // Creates a map to map unit prices by stock code

            try {
                // Reads each line and map the necessary information
                while ((line = reader.readLine()) != null) {

                    String[] values = line.split(";");

                    String invoiceDate = values[4].split(" ")[0]; 
                    String stockCode = values[1];
                    int quantity = Integer.parseInt(values[3]);
                    double unitPrice = Double.parseDouble(values[5]);
                    
                    salesData.putIfAbsent(invoiceDate, new HashMap<>());
                    Map<String, Integer> quantityMap = salesData.get(invoiceDate);
                    quantityMap.put(stockCode, quantityMap.getOrDefault(stockCode, 0) + quantity);
                    priceData.put(stockCode, unitPrice);
                }
                // Writes sales data to a new file
                String outputKey = "Sales data/sales_data_" + key; // Prefix to add a new folder
                StringBuilder outputContent = new StringBuilder("InvoiceDate;StockCode;TotalQuantity;TotalAmount\n");
                for (Map.Entry<String, Map<String, Integer>> entry : salesData.entrySet()) {

                    String invoiceDate = entry.getKey();
                    Map<String, Integer> quantityMap = entry.getValue();

                    for (Map.Entry<String, Integer> quantityEntry : quantityMap.entrySet()) {

                        String stockCode = quantityEntry.getKey();
                        int totalQuantity = quantityEntry.getValue();
                        double unitPrice = priceData.get(stockCode);
                        double totalAmount = totalQuantity * unitPrice;
                        outputContent.append(invoiceDate).append(";")
                                .append(stockCode).append(";")
                                .append(totalQuantity).append(";")
                                .append(totalAmount).append("\n");
                    }    
                }
                // Uploads the file to the output S3 bucket
                byte[] contentBytes = outputContent.toString().getBytes();
                ByteArrayInputStream inputStreamForOutput = new ByteArrayInputStream(contentBytes);
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentLength(contentBytes.length);

                s3Client.putObject(bucket, outputKey, inputStreamForOutput, metadata);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return "Sales calcultaed correctly";
    }
}