package com.example.demo;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;


import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.util.TableUtils;

// @Configuration
public class AmazonDynamoDBSample {
	 static AmazonDynamoDB dynamoDB;
	 
	 private static void init() throws Exception {
	        ProfileCredentialsProvider credentialsProvider = new ProfileCredentialsProvider();
	        try {
	            credentialsProvider.getCredentials();
	        } catch (Exception e) {
	            throw new AmazonClientException(
	                    "Cannot load the credentials from the credential profiles file. " +
	                    "Please make sure that your credentials file is at the correct " +
	                    "location (/Users/aliaksandrnenartovich/.aws/credentials), and is in valid format.",
	                    e);
	        }
	        dynamoDB = AmazonDynamoDBClientBuilder.standard()
	            .withCredentials(credentialsProvider)
	            .withRegion("us-east-1")
	            .build();
	    }
	 
	 @Bean
	 public static void createDynamo()  throws Exception {
		 init();
		 try {
	            String tableName = "Music";

	            // Create a table with a primary hash key named 'name', which holds a string
	            CreateTableRequest createTableRequest = new CreateTableRequest().withTableName(tableName)
	                .withKeySchema(new KeySchemaElement().withAttributeName("Artist").withKeyType(KeyType.HASH))
	                .withAttributeDefinitions(new AttributeDefinition().withAttributeName("Artist").withAttributeType(ScalarAttributeType.S))
	                .withProvisionedThroughput(new ProvisionedThroughput().withReadCapacityUnits(1L).withWriteCapacityUnits(1L));

	            // Create table if it does not exist yet
	            TableUtils.createTableIfNotExists((com.amazonaws.services.dynamodbv2.AmazonDynamoDB) dynamoDB, createTableRequest);
	            // wait for the table to move into ACTIVE state
	            TableUtils.waitUntilActive(dynamoDB, tableName);

	            // Describe our new table
	            DescribeTableRequest describeTableRequest = new DescribeTableRequest().withTableName(tableName);
	            TableDescription tableDescription = dynamoDB.describeTable(describeTableRequest).getTable();
	            System.out.println("Table Description: " + tableDescription);

	            // Add an item
	            Map<String, AttributeValue> item = newItem("Tool", "Pneuma", "Fear Inoculum", 2019);
	            PutItemRequest putItemRequest = new PutItemRequest(tableName, item);
	            PutItemResult putItemResult = dynamoDB.putItem(putItemRequest);

	            // Add another item
	            item = newItem("Red Hot Chili Peppers", "Aeroplane", "One Hot Minute", 1995);
	            putItemRequest = new PutItemRequest(tableName, item);
	            putItemResult = dynamoDB.putItem(putItemRequest);
	            
	            // Add another item
	            item = newItem("Nirvana", "Smells Like Teen Spirit", "Nevermind", 1991);
	            putItemRequest = new PutItemRequest(tableName, item);
	            putItemResult = dynamoDB.putItem(putItemRequest);

	            // Scan items for movies with a year attribute greater than 1985
	            HashMap<String, Condition> scanFilter = new HashMap<String, Condition>();
	            Condition condition = new Condition()
	                .withComparisonOperator(ComparisonOperator.GT.toString())
	                .withAttributeValueList(new AttributeValue().withS("1994"));
	            scanFilter.put("Year", condition);
	            ScanRequest scanRequest = new ScanRequest(tableName).withScanFilter(scanFilter);
	            ScanResult scanResult = dynamoDB.scan(scanRequest);
	            
	            // display scan results
	            List<Map<String, AttributeValue>> ar = scanResult.getItems();
	            for(Map<String, AttributeValue> map: ar) {
	            	List<Map.Entry<String, AttributeValue>> l = new LinkedList<>(map.entrySet());
	            	for(Map.Entry<String, AttributeValue> m: l)
	            		System.out.print("Key is: " + m.getKey() + "; " + "Value is " + m.getValue().getS() + "; ");
	            	System.out.println();
	            }

	        } catch (AmazonServiceException ase) {
	            System.out.println("Caught an AmazonServiceException, which means your request made it "
	                    + "to AWS, but was rejected with an error response for some reason.");
	            System.out.println("Error Message:    " + ase.getMessage());
	            System.out.println("HTTP Status Code: " + ase.getStatusCode());
	            System.out.println("AWS Error Code:   " + ase.getErrorCode());
	            System.out.println("Error Type:       " + ase.getErrorType());
	            System.out.println("Request ID:       " + ase.getRequestId());
	        } catch (AmazonClientException ace) {
	            System.out.println("Caught an AmazonClientException, which means the client encountered "
	                    + "a serious internal problem while trying to communicate with AWS, "
	                    + "such as not being able to access the network.");
	            System.out.println("Error Message: " + ace.getMessage());
	        }
	        
	        // delete Music table
	        DynamoDB DB = new DynamoDB(dynamoDB);
	        Table table = DB.getTable("Music");
	        try {
	            System.out.println("Attempting to delete table; please wait...");
	            table.delete();
	            table.waitForDelete();
	            System.out.print("Table " + table.getTableName() + " deteled successfully.");

	        }
	        catch (Exception e) {
	            System.err.println("Unable to delete table: ");
	            System.err.println(e.getMessage());
	        }
	        dynamoDB.shutdown();
	 }
	 
	 private static Map<String, AttributeValue> newItem(String artist, String song, String album, int year) {
	        Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
	        item.put("Artist", new AttributeValue(artist));
	        item.put("Song", new AttributeValue(song));
	        item.put("Album", new AttributeValue(album));
	        item.put("Year", new AttributeValue(Integer.toString(year)));
	        return item;
	    }
}
