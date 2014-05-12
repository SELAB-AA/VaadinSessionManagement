package com.example.sessionpersistenceproject.client;

import java.io.Serializable;

import com.example.sessionpersistenceproject.shared.ClientStorageClientRpc;
import com.example.sessionpersistenceproject.shared.ClientStorageServerRpc;
import com.example.sessionpersistenceproject.shared.Scope;
import com.google.gwt.storage.client.Storage;
import com.vaadin.client.ServerConnector;
import com.vaadin.client.VConsole;
import com.vaadin.client.extensions.AbstractExtensionConnector;
import com.vaadin.server.communication.ClientStorage;
import com.vaadin.shared.ui.Connect;

@Connect(ClientStorage.class)
public class ClientStorageConnector extends AbstractExtensionConnector implements Serializable {
	private ClientStorageServerRpc rpc;

	private Storage sessionStorage;
	private Storage localStorage;

	@Override
	protected void init() {
		registerRpc(ClientStorageClientRpc.class, new ClientStorageClientRpc() {

			@Override
			public void setItem(final Scope scope, final String key,
					final String value) {
				final Storage storage = getStorage(scope);
				if (storage != null) {
					storage.setItem(key, value);
				}
			}

			@Override
			public void getItem(final Scope scope, final String uuid,
					final String key) {
				final Storage storage = getStorage(scope);
				if (storage != null) {
					final String value = storage.getItem(key);
					rpc.returnValue(uuid, value);
				}
			}

			@Override
			public void removeItem(final Scope scope, final String key) {
				final Storage storage = getStorage(scope);
				if (storage != null) {
					storage.removeItem(key);
				}
			}
		});
		rpc = getRpcProxy(ClientStorageServerRpc.class);
		localStorage = Storage.getLocalStorageIfSupported();
		sessionStorage = Storage.getSessionStorageIfSupported();
		rpc.setSupport(Storage.isSupported());
	}

	@Override
	protected void extend(final ServerConnector target) {
	}

	private Storage getStorage(final Scope scope) {
		Storage storage;
		if (scope == Scope.LOCAL) {
			storage = localStorage;
		} else if (scope == Scope.SESSION) {
			storage = sessionStorage;
		} else {
			VConsole.error("Unsupported storage scope: " + scope);
			storage = null;
		}
		return storage;
	}

}
