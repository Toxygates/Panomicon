package otgviewer.client;


import java.util.List;

import otgviewer.shared.DataColumn;
import otgviewer.shared.DataFilter;
import otgviewer.shared.ExpressionRow;
import otgviewer.shared.Group;
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
	 * @param absValFilter Require that rows should contain at least one value whose 
	 * abs. value is >= this threshold. If this is 0, it will be ignored.
	 * @param loadID an integer to identify the dataset that has been loaded.
	 * @return
	 */
	public int loadDataset(DataFilter filter, List<DataColumn> columns, 
			String[] probes, ValueType type, double absValFilter);
	
	/**
	 * Add a T-test column. Requires that loadDataset was
	 * first used to load items. After this has been done, 
	 * datasetItems or getFullData can be used as normal to obtain the data.
	 * @param g1
	 * @param g2
	 */
	public void addTTest(Group g1, Group g2);
	
	/**
	 * Get one page. Requires that loadDataset was first used to load items.
	 * @param offset
	 * @param size
	 * @param sortColumn data column to sort by (0 for the first microarray, etc).
	 * If this parameter is -1, the previously applied sorting is used.
	 * @param ascending Whether to use ascending sort. Applies if sortColumn is not -1.
	 * @return
	 */
	public List<ExpressionRow> datasetItems(int offset, int size, int sortColumn, 
			boolean ascending);
	
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
