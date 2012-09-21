package otgviewer.client;

import java.util.List;

import otgviewer.shared.DataColumn;
import otgviewer.shared.DataFilter;
import otgviewer.shared.ExpressionRow;
import otgviewer.shared.Group;
import otgviewer.shared.ValueType;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface KCServiceAsync {	

	public void identifiersToProbes(DataFilter filter, String[] identifiers, AsyncCallback<String[]> callback);
	
	public void loadDataset(DataFilter filter, List<DataColumn> columns, String[] probes, 
			ValueType type, double absValFilter, AsyncCallback<Integer> callback);
	public void datasetItems(int offset, int size, int sortColumn, 
			boolean ascending, AsyncCallback<List<ExpressionRow>> callback);
	public void getFullData(DataFilter filter, List<String> barcodes, 
			String[] probes, ValueType type, boolean sparseRead, 
			AsyncCallback<List<ExpressionRow>> callback);
	
	public void prepareCSVDownload(AsyncCallback<String> callback);
	
	public void addTTest(Group g1, Group g2, AsyncCallback<Void> callback);
}
