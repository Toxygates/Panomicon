package otgviewer.client;


import java.util.List;

import otgviewer.shared.DataFilter;
import otgviewer.shared.ExpressionRow;
import otgviewer.shared.ValueType;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("KC")
public interface KCService extends RemoteService {

	public String[] identifiersToProbes(DataFilter filter, String[] identifiers);
	
	/**
	 * Load data into the user's session.
	 * @param filter
	 * @param barcodes
	 * @param probes
	 * @param type
	 * @return
	 */
	public int loadDataset(DataFilter filter, List<String> barcodes, String[] probes, ValueType type);
	
	/**
	 * Get one page. Requires that loadDataset was first used to load items.
	 * @param offset
	 * @param size
	 * @return
	 */
	public List<ExpressionRow> datasetItems(int offset, int size);
	
	/**
	 * Get all data immediately. Requires that loadDataset was first used to load items.
	 * @param filter
	 * @param barcodes
	 * @param probes
	 * @param type
	 * @param sparseRead
	 * @return
	 */
	public List<ExpressionRow> getFullData(DataFilter filter, List<String> barcodes, String[] probes, 
			ValueType type, boolean sparseRead);
	
	/**
	 * Prepare a CSV file representing the loaded data for download. Returns a URL that may be used for downloading.
	 * Requires that loadDataset was first used to load items.
	 * @return
	 */
	public String prepareCSVDownload();
}
