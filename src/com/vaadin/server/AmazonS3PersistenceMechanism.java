package com.vaadin.server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.vaadin.server.VaadinSession;


/**
 * The implementation of the PersistenceMechanism to export and import VaadinSession objects from amazon S3 service.
 */
public class AmazonS3PersistenceMechanism implements PersistenceMechanism {

	private String accessKey;
	private String secretKey;
	private String bucketName;
	
	private AmazonS3 s3client;
	
	/**
	 * 
	 * @param accessKey The access-key for a amazon S3 service
	 * @param secretKey The secret-key for a amazon S3 service
	 * @param bucketName The name of the bucket for a amazon S3 service where VaadinSessions objects are and where they should be stored.
	 */
	public AmazonS3PersistenceMechanism(String accessKey, String secretKey, String bucketName){
		this.accessKey = accessKey;
		this.secretKey = secretKey;
		this.bucketName = bucketName;
	}

	/**
	 *  Initializes the Amazon S3 client and makes the connection.
	 */
	public void initClient(){
		AWSCredentials myCredentials = new BasicAWSCredentials(accessKey, secretKey);
		s3client = new AmazonS3Client(myCredentials);			
	}

	/**
	 * Method for saving VaadinSession to Amazon S3 service
	 *
	 * @param  vsession The VaadinSession object that is to be saved externally.
 	 * @param  unique_id A unique identifier ( standard request session id)  sent by PersistenceVaadinServlet.
	 */
	public void saveSession(VaadinSession vsession, String unique_id){
		try {
			File file = File.createTempFile(unique_id, ".VaadinSession");
			FileOutputStream f_out = new FileOutputStream(file);
	        ObjectOutputStream oos;
			oos = new ObjectOutputStream(f_out);
	        oos.writeObject(vsession);
	        oos.flush();
			String FileNameInBucket = unique_id + ".VaadinSession";
			s3client.putObject(new PutObjectRequest(bucketName, FileNameInBucket, file));
	        oos.close();
			file.delete();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Method for retrieving VaadinSession from Amazon S3 service.
	 *
 	 * @param  unique_id A unique identifier to know which VaadinSession object to be retrieved.
	 */
	public Object getSession(String unique_id){
		String FileNameInBucket = unique_id + ".VaadinSession";
		try{
			S3Object s3object = s3client.getObject(new GetObjectRequest(bucketName, FileNameInBucket));
			InputStream objectData = s3object.getObjectContent();
	    	ObjectInputStream obj_in = null;
			try {
				obj_in = new ObjectInputStream (objectData);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NullPointerException e){
				e.printStackTrace();
			}

	    	Object obj = null;
			try {
				obj = (VaadinSession) obj_in.readObject();
			} catch (IOException e) {
				// TODO Auto-generated catch block

				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block

				e.printStackTrace();
			}
			catch (NullPointerException e) {
				// TODO Auto-generated catch block

				e.printStackTrace();
			}
	        
			// Return the VaadinSession as Object
			return obj;
		}
		catch (AmazonS3Exception e){
			return null;
		}
	}

}
