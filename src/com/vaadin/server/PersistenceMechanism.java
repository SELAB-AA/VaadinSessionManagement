/*
 * Copyright 2000-2013 Vaadin Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */



package com.vaadin.server;

import com.vaadin.server.VaadinSession;


/**
* PersistenceMechanism interface. A interface used to export VaadinSessions to external storage media (e.g. db connection or AMAZON S3 connection).
*/
public interface PersistenceMechanism {

	/**
	 * Method for initialization of the external storage media.
	 */
	void initClient();
	
	/**
	 * Method for saving VaadinSession to external storage media.
	 *
	 * @param  vsession The VaadinSession object that is to be saved externally.
 	 * @param  unique_id A unique identifier ( standard request session id)  sent by PersistenceVaadinServlet.
	 */
	void saveSession(VaadinSession vsession, String unique_id);
	
	/**
	 * Method for retrieving VaadinSession from external storage media.
	 *
 	 * @param  unique_id A unique identifier to know which VaadinSession object to be retrieved.
	 */
	Object getSession(String unique_id);
	
}