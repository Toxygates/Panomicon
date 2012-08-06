package otgviewer.client;


import java.util.List;

import otgviewer.shared.DataFilter;
import otgviewer.shared.ExpressionRow;
import otgviewer.shared.ValueType;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("KC")
public interface KCService extends RemoteService {

	public List<ExpressionRow> absoluteValues(DataFilter filter, String barcode);	
	public List<ExpressionRow> foldValues(DataFilter filter, String barcode);
	
	//Set up paging.
	public int loadDataset(DataFilter filter, List<String> barcodes, String[] probes, ValueType type);
	//Get one page.
	public List<ExpressionRow> datasetItems(int offset, int size);
	
	//Get all data immediately.
	public List<ExpressionRow> getFullData(DataFilter filter, List<String> barcodes, String[] probes, 
			ValueType type, boolean sparseRead);
	
	//Prepare a CSV file representing the loaded data for download. Returns a URL that may be used for downloading.
	public String prepareCSVDownload();
}
