package com.vaadin.server;

import java.util.Properties;

import com.amazonaws.services.s3.AmazonS3Client;
import com.vaadin.server.CommunicationManager;
import com.vaadin.server.DefaultDeploymentConfiguration;
import com.vaadin.server.DeploymentConfiguration;
import com.vaadin.server.ServiceException;
import com.vaadin.server.SessionExpiredException;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinServletRequest;
import com.vaadin.server.VaadinServletResponse;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedHttpSession;
import com.vaadin.server.AbstractCommunicationManager.Callback;
import com.vaadin.server.VaadinServlet.RequestType;
import com.vaadin.shared.ApplicationConstants;
import com.vaadin.ui.UI;
import com.vaadin.util.CurrentInstance;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Authenticator.RequestorType;
import java.security.GeneralSecurityException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;


/**
 * Servlet implementation class PersistenceVaadinServlet. Used as a Servlet in the Vaadin Framework to persist session using some implementation of the PersistenceMechanism interface. Configured through web.xml
 * 
 */
@SuppressWarnings("serial")
public class PersistenceVaadinServlet extends VaadinServlet {
	
	private PersistenceMechanism persistenceClient;

	//AbstractApplicationServletWrapper copied from VaadinServlet
	private static class AbstractApplicationServletWrapper implements Callback {

        private final VaadinServlet servlet;

        public AbstractApplicationServletWrapper(VaadinServlet servlet) {
            this.servlet = servlet;
        }

        @Override
        public void criticalNotification(VaadinRequest request,
                VaadinResponse response, String cap, String msg,
                String details, String outOfSyncURL) throws IOException {
            servlet.criticalNotification((VaadinServletRequest) request,
                    ((VaadinServletResponse) response), cap, msg, details,
                    outOfSyncURL);
        }
    }
	
	public PersistenceVaadinServlet(){
		super();
	}
	
	/**
	 * Overrides the default DeployementConfiguration method in VaadinServlet class. 
	 * Sets and initializes the persistenceClient attribute which should
	 * be a implementation of the interface PersistenceMechanism. 
	 *
	 * @param  initParameters init parameters configured through web.xml located in WebContent/WEB-INF
	 */
	protected DeploymentConfiguration createDeploymentConfiguration(
            Properties initParameters) {
    	String PersistenceMechanism = initParameters.getProperty("PersistenceMechanism");

    	if (PersistenceMechanism.equalsIgnoreCase("AMAZONS3")){//TODO: Propose that this could be moved and integrated into Vaadin Constants
    		String accessKey = initParameters.getProperty("accessKey");
    		String secretKey = initParameters.getProperty("secretKey");
    		String bucketName = initParameters.getProperty("bucketName");
    		persistenceClient = new AmazonS3PersistenceMechanism(accessKey,secretKey, bucketName);
    		persistenceClient.initClient();    		
    	}
    	
        return new DefaultDeploymentConfiguration(getClass(), initParameters);
    }
	
	/**
	 * Overrides the default service method in VaadinServlet class. 
	 * Used to store VaadinSession objects to PersistenceMechanism implementation if request is UIDL
	 * Used to read VaadinSession objects from PersistenceMechanism implementation if request is BROWSER_DETAILS
	 * 
	 *
	 * @param request Standard HttpServletRequest. Part of Vaadin.
	 * @param response Standard HttpServletResponse. Part of Vaadin.
	 */
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
    	// Handle context root request without trailing slash, see #9921
    	if (handleContextRootWithoutSlash(request, response)) {
            return;
        }
        CurrentInstance.clearAll();
        setCurrent(this);
        VaadinServletRequest vaadinservletrequest = new VaadinServletRequest(request, getService());
        VaadinServletResponse vaadinservletresponse = new VaadinServletResponse(response, getService());

        RequestType requestType = getRequestType(vaadinservletrequest);
        
    	if (requestType == RequestType.BROWSER_DETAILS){
        	// IF BROWSER_DETAILS check if it can be loaded from X. otherwise run super()
    		getService().setCurrentInstances(vaadinservletrequest, vaadinservletresponse);

    		String JSESSIONID = request.getSession().getId();
    		VaadinSession persistedSession = (VaadinSession) persistenceClient.getSession(JSESSIONID);	
    		if (persistedSession!=null){
    			HttpSession Httpsession = request.getSession();
    			persistedSession.storeInSession(getService(), new WrappedHttpSession(Httpsession));

    			persistedSession.getBrowser().updateRequestDetails(vaadinservletrequest);
    			persistedSession.setCommunicationManager(new PersistenceCommunicationManager(persistedSession));
                CommunicationManager communicationManager = (CommunicationManager)
                		persistedSession.getCommunicationManager();
            	communicationManager.handleBrowserDetailsRequest(vaadinservletrequest, 
            			vaadinservletresponse, persistedSession);
    		}
    		else{
    			super.service(request, response);
    		}
        }	
        else if(requestType == RequestType.UIDL){
        	// IF UIDL than handle it (send it to X to handle if should be saved or not)
        	// send super()
        	VaadinSession vSession = null;
        	try {
        		vSession = getService().findVaadinSession(vaadinservletrequest);
			} catch (ServiceException e) {
				// Let VaadinServlet handle it 
				// TODO: Feature: force reload site when sessions expires
				super.service(request, response);
			} catch (SessionExpiredException e) {
				// Let VaadinServlet handle it
				// TODO: Feature: force reload site when sessions expires
				super.service(request, response);
			}
        	super.service(request, response);
        	if (vSession!=null){
 	       		String JSESSIONID = request.getSession().getId();
 	       		persistenceClient.saveSession(vSession, JSESSIONID);
        	}
        }
        else{
        	super.service(request, response);
        }

        CurrentInstance.clearAll();
    }
}
