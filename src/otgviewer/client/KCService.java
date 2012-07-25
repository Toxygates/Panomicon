package otgviewer.client;


import java.util.List;
import java.util.Map;

import otgviewer.shared.ExpressionRow;
import otgviewer.shared.ValueType;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("KC")
public interface KCService extends RemoteService {

	public List<ExpressionRow> absoluteValues(String barcode);	
	public List<ExpressionRow> foldValues(String barcode);
	
	//Set up paging.
	public int loadDataset(List<String> barcodes, String[] probes, ValueType type);
	//Get one page.
	public List<ExpressionRow> datasetItems(int offset, int size);
	
	//Get all data immediately.
	public List<ExpressionRow> getFullData(List<String> barcodes, String[] probes, 
			ValueType type, boolean sparseRead);
}
