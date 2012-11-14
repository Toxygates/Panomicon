package otgviewer.client;


import java.util.List;

import otgviewer.shared.DataColumn;
import otgviewer.shared.DataFilter;
import otgviewer.shared.ExpressionRow;
import otgviewer.shared.Group;
import otgviewer.shared.Series;
import otgviewer.shared.Synthetic;
import otgviewer.shared.ValueType;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("KC")
public interface KCService extends RemoteService {

	/**
	 * Convert identifiers such as genes, probe IDs and proteins into a list of probes.
	 * 
	 * @param filter
	 * @param identifiers
	 * @param precise If true, names must be an exact match, otherwise partial name matching is used.
	 * @return
	 */
	public String[] identifiersToProbes(DataFilter filter, String[] identifiers, boolean precise);
	
	/**
	 * Load data into the user's session.
	 * @param filter
	 * @param barcodes
	 * @param probes
	 * @param type
	 * @param absValFilter Require that rows should contain at least one value whose 
	 * abs. value is >= this threshold. If this is 0, it will be ignored.
	 * @param synthCols Synthetic columns, such as T-Tests, that should be computed
	 * from the start.
	 * @return
	 */
	public int loadDataset(DataFilter filter, List<DataColumn> columns, 
			String[] probes, ValueType type, double absValFilter,
			List<Synthetic> synthCols);
	
	/**
	 * Add a T-test column. Requires that loadDataset was
	 * first used to load items. After this has been done, 
	 * datasetItems or getFullData can be used as normal to obtain the data.
	 * The test is two-tailed and does not assume equal sample variances.
	 * @param g1
	 * @param g2
	 */
	public void addTwoGroupTest(Synthetic.TwoGroupSynthetic test);

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
	
	/**
	 * Obtain a single time series or dose series.
	 * @param filter
	 * @param probe Must be a probe id.
	 * @param timeDose A fixed time, or a fixed dose.
	 * @param compound 
	 * @return
	 */
	public Series getSingleSeries(DataFilter filter, String probe, String timeDose, String compound);
	
	/**
	 * Obtain a number of time series or dose series.
	 * @param filter Must be specified.
	 * @param probes Must be specified. Can be genes/proteins/probe ids.
	 * @param timeDose A fixed time, or a fixed dose. Can optionally be null (no constraint).
	 * @param compound Can optionally be null (no constraint). If this is null, timeDose must be null.
	 * @return
	 */
	public List<Series> getSeries(DataFilter filter, String[] probes, String timeDose, String compound);
	
}
