package com.vaadin.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;

import org.objenesis.strategy.SerializingInstantiatorStrategy;
import org.objenesis.strategy.StdInstantiatorStrategy;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.example.sessionpersistenceproject.SessionpersistenceprojectUI;
import com.vaadin.server.communication.ClientStorage.ClientStorageSupportListener;
import com.vaadin.server.communication.ClientStorage;
import com.vaadin.server.sessionstorage.SessionStorageManager;
import com.vaadin.server.Extension;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.UI;


/**
 * The implementation of the SessionStorage interface that enables the persistence of session to HTML5-LocalStorage.
 * Needs custom vaadinbootstrap.js file that includes the session in browser_details request
 * Since there is a need to access AbstractClientConnector this class needs to exsist in com.vaadin.server instead of com.vaadin.server.sessionstorage
 * @author mrosin
 *
 */
public  class HTML5LocalStorageSessionStorageManager implements SessionStorageManager{

	/**
	 * Needed for the ClientStorage Vaadin addon
	 * @author mrosin
	 *
	 */
	class MyClientStorageSupportListener implements ClientStorageSupportListener, Serializable{

		@Override
		public void clientStorageIsSupported(boolean supported) {
			// TODO Auto-generated method stub
			if (!supported) {

			}
		}

	}

	/**
	 * The implementation for the loadSession which fetches the VaadinSession from the request. The request contains the VaadinSession as a String object. 
	 * @return VaadinSession object or null if it cannot for some reason be loaded from request
	 */
	public VaadinSession loadSession(VaadinRequest request) {
		long startloadSession = System.currentTimeMillis();
		VaadinSession vaadinSession = null;
		System.out.println(request.getParameter("v-session"));
		if (request.getParameter("v-session").equalsIgnoreCase("null")){
			return null;
		}
		try {
			vaadinSession = DeserializeFromBase64String(request.getParameter("v-session"));	
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		long stoploadSession = System.currentTimeMillis();
        return vaadinSession;
	}

	/**
	 * The implementation for the storeSession that stores the VaadinSession object into HTML5-LocalStorage.
	 * Uses the Vaadin ClientStorage addon and therefor checks if the UI associated with the session contains the addon extension and if not adds it to UI.
	 */
	public void storeSession(VaadinSession vaadinSession, int uiId, String Identifier, VaadinRequest request) {
		long startstoreSession = System.currentTimeMillis();
		long startinitClientStorage = System.currentTimeMillis();
		Collection<Extension> extensions = vaadinSession.getUIById(uiId).getExtensions();
		ClientStorage clientStorage = null;
		if(extensions.isEmpty()){
			clientStorage = new ClientStorage(new MyClientStorageSupportListener());
			((UI) vaadinSession.getUIById(uiId)).addExtension(clientStorage);
		}
		else{
			for( Extension ext : extensions){
				if(ext instanceof ClientStorage){
					clientStorage = (ClientStorage) ext;
				}
			}
			if(clientStorage==null){
				//No ClientStorage in extensions. Lets add it
				clientStorage = new ClientStorage(new MyClientStorageSupportListener());
				((UI) vaadinSession.getUIById(uiId)).addExtension(clientStorage);
			}
		}
		long stopinitClientStorage = System.currentTimeMillis();
		System.out.println("time init ClientStorage: " + (stopinitClientStorage-startinitClientStorage));
		String serialized_session_64Base = null;
		try {
			serialized_session_64Base = SerializeToBase64String(vaadinSession);
		} catch (IOException e) {
			// TODO add propriate messages to Logger: Could not serialize session
			e.printStackTrace();
		}
		if(serialized_session_64Base!=null){
			long startstoresessionClientStorage = System.currentTimeMillis();
			clientStorage.setLocalItem("v-session", serialized_session_64Base);
			long stopstoresessionClientStorage = System.currentTimeMillis();
			System.out.println("clientStorage.setLocalItem time: " + (stopstoresessionClientStorage-startstoresessionClientStorage));
		}
		long stopStoreSession = System.currentTimeMillis();
		System.out.println("Total HTML5 storesession time: " + (stopStoreSession-startstoreSession));
	}

	/**
	 * Helper method that De-serializes a base64 string into a VaadinSession object
	 * @param s The string to be de-serialized
	 * @return VaadinSession or null if process for some reason was not succesfull
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private VaadinSession DeserializeFromBase64String( String s ) throws IOException , ClassNotFoundException {		
		long startbaseconversion = System.currentTimeMillis();
		if(s.contains(" ")){// For some reason the localStorage seems to change + characters to spaces. 
			s = s.replace(" ", "+");
		}
		
		byte [] data =javax.xml.bind.DatatypeConverter.parseBase64Binary(s);
		long stopbaseconversion = System.currentTimeMillis();
		System.out.println("Time converting base 64: " + (stopbaseconversion-startbaseconversion));
		long startDeserialization = System.currentTimeMillis();
		ObjectInputStream ois = new ObjectInputStream( 
				new ByteArrayInputStream(  data ) );
		Object obj  = ois.readObject();
		ois.close();
		if(obj instanceof VaadinSession){
			long stopDeserialization = System.currentTimeMillis();
			System.out.println("Time deserializing object (HTML5): " + (stopDeserialization - startDeserialization));
			return (VaadinSession)obj;
		}
		else{
			return null;
		}
	}

	/**
	 * Helper method that serializes a object into a string in base64 format
	 * @param o A serializable object, in this case a VaadinSession
	 * @return The string representation of the object in base64 format
	 * @throws IOException
	 */
	private String SerializeToBase64String( Serializable o ) throws IOException {
		long startserialization = System.currentTimeMillis();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream( baos );
		oos.writeObject( o );
		oos.flush();
		oos.close();
		long starttobytearray = System.currentTimeMillis();
		byte [] barray = baos.toByteArray();
		long stoptobytearray = System.currentTimeMillis();
		baos.flush();
		baos.close();
		System.out.println("time for converting to byte array in serialization HTML5: " + (stoptobytearray-startserialization));
		long stopserializatinon = System.currentTimeMillis();
		System.out.println("Time for serialization HTML5: " + (stopserializatinon-startserialization));
		long startbaseconversion = System.currentTimeMillis();
		String convertedarray = javax.xml.bind.DatatypeConverter.printBase64Binary( barray );
		long stopbaseconversion = System.currentTimeMillis();
		System.out.println("Time for base 64 conversion in HTML5 localStorage: " + (stopbaseconversion-startbaseconversion));
		return new String(convertedarray);
	}
}

