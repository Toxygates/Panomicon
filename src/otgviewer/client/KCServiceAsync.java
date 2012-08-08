package otgviewer.client;

import java.util.List;

import otgviewer.shared.DataFilter;
import otgviewer.shared.ExpressionRow;
import otgviewer.shared.ValueType;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface KCServiceAsync {	

	public void identifiersToProbes(DataFilter filter, String[] identifiers, AsyncCallback<String[]> callback);
	
	public void loadDataset(DataFilter filter, List<String> barcodes, String[] probes, ValueType type, AsyncCallback<Integer> callback);
	public void datasetItems(int offset, int size, AsyncCallback<List<ExpressionRow>> callback);
	public void getFullData(DataFilter filter, List<String> barcodes, String[] probes, ValueType type, boolean sparseRead, 
			AsyncCallback<List<ExpressionRow>> callback);
	
	public void prepareCSVDownload(AsyncCallback<String> callback);
}
