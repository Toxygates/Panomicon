package gwttest.client;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface OwlimServiceAsync {

	public void compounds(AsyncCallback<String[]> callback);
	
	public void organs(String compound, AsyncCallback<String[]> callback);
	
	public void doseLevels(String compound, String organ, AsyncCallback<String[]> callback);
}
