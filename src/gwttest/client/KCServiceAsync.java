package gwttest.client;

import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;


public interface KCServiceAsync {

	public void absoluteValues(String barcode, AsyncCallback<List<ExpressionRow>> callback);	
	public void foldValues(String barcode, AsyncCallback<Map<String, Double>> callback);
}
