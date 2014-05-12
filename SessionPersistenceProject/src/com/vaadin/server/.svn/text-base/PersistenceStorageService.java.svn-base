package com.vaadin.server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import com.vaadin.server.communication.FileUploadHandler;
import com.vaadin.server.communication.HeartbeatHandler;
import com.vaadin.server.communication.PersistenceUIInitHandler;
import com.vaadin.server.communication.PublishedFileHandler;
import com.vaadin.server.communication.PushRequestHandler;
import com.vaadin.server.communication.ServletBootstrapHandler;
import com.vaadin.server.communication.SessionRequestHandler;
import com.vaadin.server.communication.UidlRequestHandler;
import com.vaadin.server.sessionstorage.AmazonS3SessionStorageManager;
import com.vaadin.server.sessionstorage.SessionStorageManager;


/**
 * The PersistenceStorageService extending the VaadinServletService modifying the session management
 * to achieve session persistence to different medias.
 * 
 * @author mrosin
 *
 */
public class PersistenceStorageService extends VaadinServletService {
	
	private SessionStorageManager sessionStorage; 
	
	public PersistenceStorageService(VaadinServlet servlet,
			DeploymentConfiguration deploymentConfiguration)
			throws ServiceException {
		super(servlet, deploymentConfiguration);
		initSessionStorage();
	}
	
	/**
	 * The initSessionStorage() method takes the SessionStorage parameter from web.xml deployment descriptor
	 * and instantiates the sessionstorage to the implementation sepcified
	 */
	private void initSessionStorage(){
		try {
			Class<?> clazz = Class.forName(this.getServlet().getInitParameter("SessionStorage"));
			Object sessionstorage = clazz.newInstance();
			sessionStorage = (SessionStorageManager) sessionstorage;
		} catch (InstantiationException e) {
			System.out.println("Error in web.xml configuration");
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			System.out.println("Error in web.xml configuration");
			e.printStackTrace();
		} catch (ClassNotFoundException e1) {
			System.out.println("Error in web.xml configuration");
			e1.printStackTrace();
		}
	}
	
	/**
	 * The getExsistingSession that uses the sessionStorage to fetch VaadinSession objects 
	 * based on the JSESSIONID from a storage media at BROWSER_DETAILS request for a client.
	 * The method sends the VaadinSession objects when a UIDL request has been made to the implementation of
	 * the sessionStorage.
	 */
	protected VaadinSession getExistingSession(VaadinRequest request,
			boolean allowSessionCreation) throws SessionExpiredException {
		String uiIdRequestParameter = request.getParameter("v-uiId");
		String browserDetailsParameter =  request.getParameter("v-browserDetails");
		VaadinSession vaadinSession = null;	
		if(browserDetailsParameter!=null){ // A BROWSER_DETAILS request has been made
			long startloadtime = System.currentTimeMillis();
			vaadinSession = sessionStorage.loadSession(request);
			System.out.println("vaadinSession: " + vaadinSession);
			long stoploadtime = System.currentTimeMillis();
			if(vaadinSession!=null){
				System.out.println("Total load Session time: " + (stoploadtime-startloadtime));

				WrappedSession wrappedSession = request.getWrappedSession();
				if(((ReentrantLock) wrappedSession.getAttribute(getLockAttributeName())).isHeldByCurrentThread()){//Current wrappedSession and current thread belongs to lock. Add lock to the VaadinSession
					wrappedSession.setAttribute(getLockAttributeName(), wrappedSession.getAttribute(getLockAttributeName()));
				}
				vaadinSession.storeInSession(getCurrent(), request.getWrappedSession());
				return vaadinSession;
			}
		}
		
		vaadinSession = super.getExistingSession(request, allowSessionCreation); // Let default method take care of fetching from RAM and creating of session
		if (vaadinSession!=null){// If vaadinSession not in memory check persistenceStorage, if this is null the super.doFindOrCreateVaadinSession will create and register a new Session
			if(uiIdRequestParameter!=null){ //"UIDL-request": sending session to sessionStorage to be serialized and stored
				long startstoretime = System.currentTimeMillis();
				sessionStorage.storeSession(vaadinSession, Integer.parseInt(uiIdRequestParameter), vaadinSession.getSession().getId(), request);
				long stopstoretime = System.currentTimeMillis();
				//System.out.println("Total store time: " + (stopstoretime-startstoretime));
			}
		}
		return vaadinSession;
	}

	
	/**
	 * Returns the name used to store the lock in the HTTP session.
	 * 
	 * @return The attribute name for the lock
	 */
	private String getLockAttributeName() {
		return getServiceName() + ".lock";
	}

	/**
	 * The method for creating RequestHandlers. Private methods from VaadinService 
	 * and VaadinServletService (merged) but overridden to use custom UIInitHandler.
	 */
	protected List<RequestHandler> createRequestHandlers()
			throws ServiceException {
		ArrayList<RequestHandler> handlers = new ArrayList<RequestHandler>();
		handlers.add(new SessionRequestHandler());
		handlers.add(new PublishedFileHandler());
		handlers.add(new HeartbeatHandler());
		handlers.add(new FileUploadHandler());
		handlers.add(new UidlRequestHandler());
		handlers.add(new UnsupportedBrowserHandler());
		handlers.add(new ConnectorResourceHandler());

		List<RequestHandler> handlers_from_servletService = handlers;
		handlers_from_servletService.add(0, new ServletBootstrapHandler());
		handlers_from_servletService.add(new PersistenceUIInitHandler());
		if (ensurePushAvailable()) {
			handlers_from_servletService.add(new PushRequestHandler(this));
		}
		return handlers;
	}

}
