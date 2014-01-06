package otgviewer.client.rpc;


import java.util.List;

import otgviewer.shared.DataFilter;
import otgviewer.shared.Group;
import otgviewer.shared.ManagedMatrixInfo;
import otgviewer.shared.Synthetic;
import otgviewer.shared.ValueType;
import bioweb.shared.array.ExpressionRow;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * This service obtains expression data from the 
 * underlying data store.
 * @author johan
 *
 */
@RemoteServiceRelativePath("matrix")
public interface MatrixService extends RemoteService {

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
	 * Load data into the user's session. Also perform an initial filtering.
	 * @param filter
	 * @param barcodes
	 * @param probes
	 * @param type
	 * @param absValFilter Require that rows should contain at least one value whose 
	 * abs. value is >= this threshold. If this is 0, it will be ignored.
	 * @param synthCols Synthetic columns, such as T-Tests, that should be computed
	 * from the start.
	 * @return The number of rows that remain after filtering.
	 */
	public ManagedMatrixInfo loadDataset(DataFilter filter, List<Group> columns, 
			String[] probes, ValueType type, double absValFilter,
			List<Synthetic> synthCols);
	
	/**
	 * Filter data that has already been loaded into the session.
	 * @param probes Probes to keep
	 * @param absValFilter Value cutoff (each row must contain at least one value such that abs(x) > this)
	 * @return The number of rows that remain after filtering.
	 */
	public ManagedMatrixInfo refilterData(String[] probes, double absValFilter);	
	
	/**
	 * Add a T-test/U-test/fold change difference column. Requires that loadDataset was
	 * first used to load items. After this has been done, 
	 * datasetItems or getFullData can be used as normal to obtain the data.
	 * The test is two-tailed and does not assume equal sample variances.
	 * @param g1
	 * @param g2
	 */
	public void addTwoGroupTest(Synthetic.TwoGroupSynthetic test);

	/**
	 * Remove all test columns. The result will be reflected in subsequent calls to
	 * datasetItems or getFullData.
	 */
	public void removeTwoGroupTests();
	
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
	 * Get all data immediately. 
	 * @param filter
	 * @param barcodes
	 * @param probes
	 * @param type 
	 * @param sparseRead If true, we optimise for the case of reading a single probe from multiple arrays.
	 * If false, we optimise for reading full arrays. 
	 * @param withSymbols If true, gene IDs and gene symbols will also be loaded into the rows (may be slightly slower)
	 * @return
	 */
	public List<ExpressionRow> getFullData(DataFilter filter, List<String> barcodes, String[] probes, 
			ValueType type, boolean sparseRead, boolean withSymbols);
	
	/**
	 * Prepare a CSV file representing the loaded data for download. Returns a URL that may be used for downloading.
	 * Requires that loadDataset was first used to load items.
	 * @return
	 */
	public String prepareCSVDownload();
	
	/**
	 * Get the GeneIDs currently being displayed. If limit is -1, no limit will be applied.
	 */
	public String[] getGenes(int limit);

}
