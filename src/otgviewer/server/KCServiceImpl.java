package otgviewer.server;

import java.util.ArrayList;
import java.util.Arrays;
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
import otgviewer.shared.DataFilter;
import otgviewer.shared.ExpressionRow;
import otgviewer.shared.ExpressionValue;
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
		if (probes == null) {
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
	
	private ExprValue[][] getExprValues(DataFilter filter, List<String> barcodes, String[] probes,
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
	
	public int loadDataset(DataFilter filter, List<String> barcodes, String[] probes, ValueType type) {
		HttpServletRequest request = getThreadLocalRequest();
		HttpSession session = request.getSession();
		
		String[] realProbes = filterProbes(filter, probes);
		ExprValue[][] r = getExprValues(filter, barcodes, realProbes, type, false);

		session.setAttribute("dataset", r);
		session.setAttribute("datasetProbes", realProbes);
		session.setAttribute("datasetBarcodes", barcodes.toArray(new String[0]));
		if (r.length > 0) {
			System.out.println("Stored " + r.length + " x " + r[0].length
					+ " items in session, " + realProbes.length + " probes");
		} else {
			System.out.println("Stored empty data in session");
		}
		return r.length;
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
				List<String> probeTitles = B2RAffy.titlesForJava((String[]) Arrays.copyOfRange(probes, offset, offset+size));
				List<String[]> geneIds = B2RAffy.geneIdsForJava((String[]) Arrays.copyOfRange(probes, offset, offset+size));
				List<String[]> geneSyms = B2RAffy.geneSymsForJava((String[]) Arrays.copyOfRange(probes, offset, offset+size));
				for (int i = offset; i < offset + size && i < probes.length && i < data.length; ++i) {					
					r.add(arrayToRow(probes[i], probeTitles.get(i - offset), geneIds.get(i - offset), 
							geneSyms.get(i - offset), data[i]));					
				}
			} finally {
				B2RAffy.close();
			}
		}

		return r;
	}
	
	private int _sortColumn;
	private boolean _sortAscending;
	public List<ExpressionRow> datasetItems(int offset, int size, int sortColumn, boolean ascending) {
		HttpServletRequest request = getThreadLocalRequest();
		HttpSession session = request.getSession();
		_sortColumn = sortColumn;
		_sortAscending = ascending;
		
		ExprValue[][] data = (ExprValue[][]) session.getAttribute("dataset");		
		if (data != null) {
			System.out.println("I had " + (data).length + " rows stored");
		}
		if (sortColumn > -1) {
			//OK, we need to re-sort it and then re-store it
			Arrays.sort(data, new Comparator<ExprValue[]>() {
				public int compare(ExprValue[] r1, ExprValue[] r2) {
					assert(r1 != null);
					assert(r2 != null);
					int c = ((Double) r1[_sortColumn].value()).compareTo((Double) r2[_sortColumn].value());
					if (_sortAscending) {
						return c;
					} else {
						return -c;
					}
				}
			});
			session.setAttribute("dataset", data);			
		}		
		String[] probes = (String[]) session.getAttribute("datasetProbes");
		return arrayToRows(probes, data, offset, size);
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
		ExprValue[][] data = (ExprValue[][]) session.getAttribute("dataset");		
		if (data != null) {
			System.out.println("I had " + (data).length + " rows stored");
		}
		String[] probes = (String[]) session.getAttribute("datasetProbes");
		String[] barcodes = (String[]) session.getAttribute("datasetBarcodes"); //todo: more helpful names would be good
		return CSVHelper.writeCSV(probes, barcodes, data);	
	}
}
