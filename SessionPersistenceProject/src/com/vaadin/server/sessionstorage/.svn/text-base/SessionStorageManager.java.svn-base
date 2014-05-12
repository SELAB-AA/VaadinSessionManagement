package com.vaadin.server.sessionstorage;
import java.io.Serializable;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinSession;

/**
 * The SessionStorage interface is used in the PersistenceStorageService and is the interface that enables
 * storing VaadinSession objects to storage medias (like for instance Amazon S3 service).
 * @author mrosin
 *
 */
public interface SessionStorageManager extends Serializable {
	/**
	 * The loadSession method that is called when a session is to be fetched from a storage media.
	 * 
	 * @param request The VaadinRequest that was associated with the call from the service. Contains all the request information
	 * @return The VaadinSession or null if none was found
	 */
	VaadinSession loadSession(VaadinRequest request);
	
	/**
	 * The storeSession method is called when a session should be stored into a storage media. 
	 * @param vaadinSession The VaadinSession object to store
	 * @param uiId The UI objects id that from which a request was made
	 * @param Identifier The identifier that identifies the client and to which the Session will be mapped to in the storage media.
	 * @param request The VaadinRequest that was associated with the call from the service. Contains all the request information
	 */
	void storeSession(VaadinSession vaadinSession, int uiId, String Identifier, VaadinRequest request);
}
