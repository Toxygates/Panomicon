package otgviewer.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import kyotocabinet.DB;
import otg.B2RAffy;
import otg.CSVHelper;
import otg.ExprValue;
import otg.OTGMisc;
import otg.OTGQueries;
import otg.Species;
import otgviewer.client.KCService;
import otgviewer.shared.DataColumn;
import otgviewer.shared.DataFilter;
import otgviewer.shared.ExpressionRow;
import otgviewer.shared.ExpressionValue;
import otgviewer.shared.Group;
import otgviewer.shared.Synthetic;
import otgviewer.shared.ValueType;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class KCServiceImpl extends RemoteServiceServlet implements KCService {

	//Future: keep connection open, close on shutdown.
	
	private DB foldsDB, absDB;
	
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		String homePath = System.getProperty("otg.home");
		
		foldsDB = OTGQueries.open(homePath + "/otgf.kct");
		absDB = OTGQueries.open(homePath + "/otg.kct");
		System.out.println("KC databases are open");
	}
	
	public void destroy() {		
		System.out.println("Closing KC databases");
		foldsDB.close();
		absDB.close();
		super.destroy();
	}	

	private String[] filterProbes(DataFilter filter, String[] probes) {	
		String[] realProbes = probes;
		Species s = Utils.speciesFromFilter(filter);
		if (probes == null || probes.length == 0) {
			// get all probes
			realProbes = OTGQueries.probeIds(s);
		} else {
			realProbes = OTGQueries.filterProbes(probes, s);					
		}
		System.out.println(realProbes.length
				+ " probes requested after filtering");
		return realProbes;
	}
	
	public String[] identifiersToProbes(DataFilter filter, String[] identifiers) {
		//convert identifiers such as proteins, genes etc to probes.
		Species s = Utils.speciesFromFilter(filter);
		String[] r = OTGMisc.identifiersToProbes(s, identifiers);
		System.out.println("Converted " + identifiers.length + " into " + r.length + " probes.");
		return r;
	}
	
	private ExprValue[][] getExprValues(DataFilter filter, Collection<String> barcodes, String[] probes,
			ValueType type, boolean sparseRead) {
		DB db = null;
		if (barcodes == null) {
			return new ExprValue[0][0];
		}

		switch (type) {
		case Folds:
			db = foldsDB;
			break;
		case Absolute:
			db = absDB;
			break;
		}
		return OTGQueries.presentValuesByBarcodesAndProbes4J(db, barcodes,
				probes, sparseRead, Utils.speciesFromFilter(filter));
	}
	
	public int loadDataset(DataFilter filter, List<DataColumn> columns, String[] probes, 
			ValueType type, double absValFilter, List<Synthetic> syntheticColumns) {
		HttpServletRequest request = getThreadLocalRequest();
		HttpSession session = request.getSession();

		long time = System.currentTimeMillis();
		
		//Expand groups into simple barcodes
		String[] orderedBarcodes = KCServiceImplS.barcodes4J(columns);

		String[] realProbes = filterProbes(filter, probes);
		ExprValue[][] data = getExprValues(filter, Arrays.asList(orderedBarcodes), realProbes, type,
				false);
		
		System.out.println("Loaded in " + (System.currentTimeMillis() - time) + " ms");
		time = System.currentTimeMillis();
		
		//Compute groups
		ExprValue[][] groupedFiltered = KCServiceImplS.computeRows4J(columns, data, orderedBarcodes);
		assert(groupedFiltered.length == data.length);
		
		// filter by abs. value
		List<ExprValue[]> remainingGrouped = new ArrayList<ExprValue[]>();
		List<ExprValue[]> remainingUngrouped = new ArrayList<ExprValue[]>();
		List<String> remainingProbes = new ArrayList<String>();
		for (int r = 0; r < groupedFiltered.length; ++r) {
			for (int i = 0; i < groupedFiltered[r].length; ++i) {
				if ((Double.isNaN(groupedFiltered[r][i].value()) && absValFilter == 0) || 
						Math.abs(groupedFiltered[r][i].value()) >= absValFilter - 0.0001) { //safe comparison
					remainingGrouped.add(groupedFiltered[r]);
					remainingUngrouped.add(data[r]);
					remainingProbes.add(realProbes[r]);
					break;
				} else {
					System.out.println("Ignore value " + groupedFiltered[r][i].value());
				}
			}
		}
		groupedFiltered = remainingGrouped.toArray(new ExprValue[0][]);
		ExprValue[][] ungroupedFiltered = remainingUngrouped.toArray(new ExprValue[0][]);
		realProbes = remainingProbes.toArray(new String[0]);

		System.out.println("Rendered in " + (System.currentTimeMillis() - time) + " ms");
		
		session.setAttribute("flatBarcodes", orderedBarcodes);
		
		//This contains the grouped and filtered data, as well as synthetic columns (eventually)
		session.setAttribute("groupedFiltered", groupedFiltered);
		 //This contains all the data as flat barcodes (filtered, not grouped, no synthetic columns)
		session.setAttribute("ungroupedFiltered", ungroupedFiltered);
		
		session.setAttribute("datasetProbes", realProbes);
		session.setAttribute("datasetColumns", columns.toArray(new DataColumn[0]));
		if (groupedFiltered.length > 0) {

			System.out.println("Stored " + groupedFiltered.length + " x "
					+ groupedFiltered[0].length + " items in session, "
					+ realProbes.length + " probes");

		} else {
			System.out.println("Stored empty data in session");
		}
		
		DataViewParams params = (DataViewParams) session.getAttribute("dataViewParams");
		if (params == null) {
			params = new DataViewParams();
		}		
		params.mustSort = true;
		session.setAttribute("params", params);

		for (Synthetic s : syntheticColumns) {
			if (s instanceof Synthetic.TTest) {
				Synthetic.TTest tt = ((Synthetic.TTest) s);
				addTTest(tt.getGroup1(), tt.getGroup2());
			}
		}

		return groupedFiltered.length;
	}
	
	/**
	 * Add a column with a two-tailed t-test that does not assume
	 * equal sample variances.
	 */
	public void addTTest(Group g1, Group g2) {
		HttpServletRequest request = getThreadLocalRequest();
		HttpSession session = request.getSession();

		long time = System.currentTimeMillis();
		
		DataColumn[] columns = (DataColumn[]) session.getAttribute("datasetColumns");
		ExprValue[][] data = (ExprValue[][]) session.getAttribute("ungroupedFiltered");
		ExprValue[][] rendered = (ExprValue[][]) session.getAttribute("groupedFiltered");		
		String[] orderedBarcodes = (String[]) session.getAttribute("flatBarcodes");		
		rendered = KCServiceImplS.performTTests(g1, g2, data, rendered, orderedBarcodes);
		System.out.println("Performed t-test in " + (System.currentTimeMillis() - time) + " ms");
//		DataViewParams params = (DataViewParams) session.getAttribute("dataViewParams");
		
		DataColumn[] ncolumns = Arrays.copyOf(columns, columns.length + 1);
		ncolumns[columns.length] = new Synthetic.TTest(g1, g2); 
		session.setAttribute("groupedFiltered", rendered);
		session.setAttribute("datasetColumns", ncolumns);
	}
	
	private ExpressionRow arrayToRow(String probe, String title, String[] geneIds, String[] geneSyms, ExprValue[] vals) {
		ExpressionValue[] vout = new ExpressionValue[vals.length];

		for (int j = 0; j < vals.length; ++j) {
			vout[j] = new ExpressionValue(vals[j].value(), vals[j].call());						
		}
		return new ExpressionRow(probe, title, geneIds, geneSyms, vout);
	}
	
	private List<ExpressionRow> arrayToRows(String[] probes, ExprValue[][] data, int offset, int size) {
		List<ExpressionRow> r = new ArrayList<ExpressionRow>();

		if (probes != null && data != null) {
			try {
				B2RAffy.connect();				
				int cpend = offset + size;
				if (cpend > probes.length) {
					cpend = probes.length; 
				}				
				System.out.println("Range: " + offset + " to " + cpend);
				
				if (cpend > offset) {
					List<String> probeTitles = B2RAffy.titles4J((String[]) Arrays.copyOfRange(probes, offset, cpend));
					List<String[]> geneIds = B2RAffy.geneIds4J((String[]) Arrays.copyOfRange(probes, offset, cpend));
					List<String[]> geneSyms = B2RAffy.geneSyms4J((String[]) Arrays.copyOfRange(probes, offset, cpend));
					for (int i = offset; i < offset + size && i < probes.length && i < data.length; ++i) {					
						r.add(arrayToRow(probes[i], probeTitles.get(i - offset), geneIds.get(i - offset), 
								geneSyms.get(i - offset), data[i]));					
					}
				}
			} finally {
				B2RAffy.close();
			}
		}

		return r;
	}

	public List<ExpressionRow> datasetItems(int offset, int size, int sortColumn, 
			boolean ascending) {
		HttpServletRequest request = getThreadLocalRequest();
		HttpSession session = request.getSession();
		
		DataViewParams params = (DataViewParams) session.getAttribute("dataViewParams");
		if (params == null) {
			params = new DataViewParams();
		}
		ExprValue[][] groupedFiltered = (ExprValue[][]) session.getAttribute("groupedFiltered");		
		if (groupedFiltered != null) {
			System.out.println("I had " + (groupedFiltered).length + " rows stored");
		}
		ExprValue[][] ungroupedFiltered = (ExprValue[][]) session.getAttribute("ungroupedFiltered");
		
		String[] probes = (String[]) session.getAttribute("datasetProbes");
		
		//At this point sorting may happen		
		if (sortColumn > -1 && (sortColumn != params.sortColumn || ascending != params.sortAsc || params.mustSort)) {			
			//OK, we need to re-sort it and then re-store it
			params.sortColumn = sortColumn;			
			params.sortAsc = ascending;			
			params.mustSort = false;
			
			//Note: these two must be sorted together since some algos assume
			//that they are kept in sync
			ExprValue[][][] sorted = KCServiceImplS.sortData4J(groupedFiltered, sortColumn, ascending, ungroupedFiltered);
//			
			session.setAttribute("groupedFiltered", sorted[0]);
			session.setAttribute("ungroupedFiltered", sorted[1]);
			groupedFiltered = sorted[0];
			String[] sortedProbes = new String[groupedFiltered.length];
			for (int i = 0; i < groupedFiltered.length; ++i) {
				sortedProbes[i] = groupedFiltered[i][0].probe();
			}
			session.setAttribute("datasetProbes", sortedProbes);
			probes = sortedProbes;			
		}				
		
		session.setAttribute("dataViewParams", params);
		
		return arrayToRows(probes, groupedFiltered, offset, size);
	}
	
	public List<ExpressionRow> getFullData(DataFilter filter, List<String> barcodes, 
			String[] probes, ValueType type, boolean sparseRead) {
		String[] realProbes = filterProbes(filter, probes);
		ExprValue[][] r = getExprValues(filter, barcodes, realProbes, type, sparseRead);
		return arrayToRows(realProbes, r, 0, r.length);
		
	}
	
	public String prepareCSVDownload() {
		HttpServletRequest request = getThreadLocalRequest();
		HttpSession session = request.getSession();
		ExprValue[][] data = (ExprValue[][]) session.getAttribute("groupedFiltered");		
		if (data != null) {
			System.out.println("I had " + (data).length + " rows stored");
		}
		String[] probes = (String[]) session.getAttribute("datasetProbes");
		DataColumn[] cols = (DataColumn[]) session.getAttribute("datasetColumns"); //todo: more helpful names would be good
		String[] colNames = new String[cols.length];
		for (int i = 0; i < cols.length; ++i) {
			colNames[i] = cols[i].toString();
		}
		return CSVHelper.writeCSV(probes, colNames, data);	
	}
}
