package com.vaadin.server.sessionstorage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Properties;
import java.util.*;
import org.objenesis.strategy.SerializingInstantiatorStrategy;
import org.objenesis.strategy.StdInstantiatorStrategy;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServletRequest;
import com.vaadin.server.VaadinSession;
/**
 * The implementation of the SessionStorage interface that enables the persistence of VaadinSessions to
 * a Amazon S3 service.
 * @author mrosin
 *
 */
public class AmazonS3SessionStorageManager implements SessionStorageManager {
	
	private String accesKey;
	private String secretKey;
	private String bucketName;
	private AmazonS3 client;

	public AmazonS3SessionStorageManager(){
		
	}
	
	/**
	 * The method for initializing and making the connection to a amazon s3 service.
	 * @param request The VaadinRequest in which the servlet init parameters contains the credential parameters needed to make a connection
	 */
	public void initAmazonClient(VaadinRequest request){
		long startinitAmazontime = System.currentTimeMillis();
		Properties initParameters = request.getService().getDeploymentConfiguration().getInitParameters();
		this.accesKey = initParameters.getProperty("accessKey");
		this.secretKey = initParameters.getProperty("secretKey");
		this.bucketName = initParameters.getProperty("bucketName");
		AWSCredentials myCredentials = new BasicAWSCredentials(accesKey, secretKey);
		client = new AmazonS3Client(myCredentials);	
		long stopinitAmazontime = System.currentTimeMillis();
		System.out.println("Total Amazon S3 initTime" + (stopinitAmazontime - startinitAmazontime));
	}
	
	
	/**
	 * The implementation for the loadSession which fetches the VaadinSession from the S3 service based on the JSESSIONID.
	 * @return VaadinSession object or null if it cannot for some reason be loaded from S3
	 */
	public VaadinSession loadSession(VaadinRequest request) {
		initAmazonClient(request);

		String FileNameInBucket = request.getWrappedSession().getId() + ".VaadinSession";
		String serializationMechanism = request.getService().getDeploymentConfiguration().getInitParameters().getProperty("serializationmechanism");
		if(serializationMechanism.equalsIgnoreCase("kryo")){
			try{
				System.out.println("Deserialization with kryo");
				Kryo kryo = new Kryo();
				kryo.setInstantiatorStrategy(new SerializingInstantiatorStrategy());
				S3Object s3object = client.getObject(new GetObjectRequest(bucketName, FileNameInBucket));
				InputStream objectData = s3object.getObjectContent();
				Input input = new Input(objectData);
				VaadinSession obj = (VaadinSession) kryo.readClassAndObject(input);
				input.close();
				return obj;
			}
			catch (Exception e){
				e.printStackTrace();
				return null;
			}
	    	
		}
		else{
			try{
				long startloaddownloadamazon = System.currentTimeMillis();
				S3Object s3object = client.getObject(new GetObjectRequest(bucketName, FileNameInBucket));
				long stoploaddownloadamazon = System.currentTimeMillis();
				System.out.println("Total download object from amazon " + (stoploaddownloadamazon-startloaddownloadamazon));
				long startloadamazondeserialization = System.currentTimeMillis();
				InputStream objectData = s3object.getObjectContent();
		    	ObjectInputStream obj_in = new ObjectInputStream (objectData);
		    	Object obj = (VaadinSession) obj_in.readObject();
		    	long stoploadamazondeserialization = System.currentTimeMillis();
		    	System.out.println("Total amazon deserialization java time " + (stoploadamazondeserialization-startloadamazondeserialization));
		    	if(obj instanceof VaadinSession){
					return (VaadinSession)obj;
				}
				else{
					return null; // The object fetched is not an VaadinSession
				}
			}
			catch(Exception e){
				e.printStackTrace();
				return null; // Something went wrong. Not returning the session. Future work should probably remove session from amazon at this point
			}
		}
	}

	/**
	 * The implementation for the storeSession that sends the VaadinSession object to S3
	 */
	public void storeSession(VaadinSession vaadinSession, int uiId, String Identifier, VaadinRequest request) {
		initAmazonClient(request);
		try {
			String serializationMechanism = request.getService().getDeploymentConfiguration().getInitParameters().getProperty("serializationmechanism");
			if(serializationMechanism.equalsIgnoreCase("kryo")){
				long startamazonstoreserializationkryo = System.currentTimeMillis();
		    	Kryo kryo = new Kryo();
		    	kryo.setInstantiatorStrategy(new SerializingInstantiatorStrategy());
		    	File file = File.createTempFile(Identifier, ".VaadinSession");
		        FileOutputStream f_out = new FileOutputStream(file);
		        Output output = new Output(f_out);
		        kryo.writeClassAndObject(output, vaadinSession);
		        output.close();
		        long stopamazonstoreserializationkryo = System.currentTimeMillis();
		        System.out.println("Total store amazon serialization kryo : " + (stopamazonstoreserializationkryo-startamazonstoreserializationkryo));
		        long startamazonputobjectkryo = System.currentTimeMillis();
		        String FileNameInBucket = Identifier + ".VaadinSession";
		        client.putObject(new PutObjectRequest(bucketName, FileNameInBucket, file));
				long stopamazonputobjectkryo = System.currentTimeMillis();
				System.out.println("total store amazon upload time kryo: " + (stopamazonputobjectkryo-startamazonputobjectkryo));
			}
			else{
				long startamazonstoreserialization = System.currentTimeMillis();
				File file = File.createTempFile(Identifier, ".VaadinSession");
				FileOutputStream f_out = new FileOutputStream(file);
				ObjectOutputStream oos =  new ObjectOutputStream(f_out);
		        oos.writeObject(vaadinSession);
		        oos.flush();
		        long stopamazonstoreserialization = System.currentTimeMillis();
		        System.out.println("total store amazon serialization: " + (stopamazonstoreserialization-startamazonstoreserialization));
				String FileNameInBucket = Identifier + ".VaadinSession";
				long startamazonputobject = System.currentTimeMillis();
				client.putObject(new PutObjectRequest(bucketName, FileNameInBucket, file));
				System.out.println(file.length());
				long stopamazonputobject = System.currentTimeMillis();
				System.out.println("total store amazon upload time: " + (stopamazonputobject-startamazonputobject));
		        oos.close();
				file.delete();
			}
		
		} catch (Exception e) {
			// Something went wrong. Session could not be saved.
			e.printStackTrace();
		}
	}
}
