package gwttest.client;

import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import gwttest.shared.*;

public interface KCServiceAsync {	
	public void absoluteValues(String barcode, AsyncCallback<List<ExpressionRow>> callback);	
	public void foldValues(String barcode, AsyncCallback<List<ExpressionRow>> callback);
	
	public void loadDataset(List<String> barcodes, String[] probes, ValueType type, AsyncCallback<Integer> callback);
	public void datasetItems(int offset, int size, AsyncCallback<List<ExpressionRow>> callback);
}
