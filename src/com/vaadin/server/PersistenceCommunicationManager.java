package com.vaadin.server;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import org.json.JSONException;
import org.json.JSONObject;

import com.vaadin.server.CommunicationManager;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinSession;
import com.vaadin.shared.ui.ui.UIConstants;
import com.vaadin.ui.UI;



/**
 * The default CommunicationManager class used by the PersistenceVaadinServlet.
 * Needed to override the handleBrowserDetailsRequest function that creates and renders UI objects
 */
public class PersistenceCommunicationManager extends CommunicationManager {

	public PersistenceCommunicationManager(VaadinSession session) {
		super(session);
	}

	/**
	 * Overrides the default Vaadin handleBrowserDetailsRequests.
	 * Instead of creating new UI instances this methoda takes UI objects from a session and renders the 
	 * UI object with ID 0. TODO: STILL UNDER CONSTRUCTION
	 *
	 * @param request  The VaadinRequest associated with the session
	 * @param response The VaadinResponse associated with the session
	 * @param session The VaadinSession from external storage given by the implemetation of PersistenceMechanism.
	 */
	@Override
	public void handleBrowserDetailsRequest(VaadinRequest request,
            VaadinResponse response, VaadinSession session) throws IOException {

        session.lock();

        try {
            assert UI.getCurrent() == null;

            response.setContentType("application/json; charset=UTF-8");

            UI uI = session.getUIById(0); //TODO: Only supports recreation of first instance of UI. Check with Ivan requirements depending on utilities.

            JSONObject params = new JSONObject();
            params.put(UIConstants.UI_ID_PARAMETER, uI.getUIId());
            String initialUIDL = getInitialUIDL(request, uI);
            params.put("uidl", initialUIDL);

            // NOTE! GateIn requires, for some weird reason, getOutputStream
            // to be used instead of getWriter() (it seems to interpret
            // application/json as a binary content type)
            final OutputStream out = response.getOutputStream();
            final PrintWriter outWriter = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(out, "UTF-8")));

            outWriter.write(params.toString());
            // NOTE GateIn requires the buffers to be flushed to work
            outWriter.flush();
            out.flush();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            session.unlock();
        }
    }

}
