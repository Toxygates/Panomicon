package otgviewer.server;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import kyotocabinet.DB;
import otg.B2RAffy;
import otg.ExprValue;
import otg.OTGQueries;
import otgviewer.client.KCService;
import otgviewer.shared.ExpressionRow;
import otgviewer.shared.ExpressionValue;
import otgviewer.shared.ValueType;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class KCServiceImpl extends RemoteServiceServlet implements KCService {

	//Future: keep connection open, close on shutdown.
	
	public List<ExpressionRow> absoluteValues(String barcode) {
		return valuesFromDb(barcode, "otg.kct");		
	}
	
	public List<ExpressionRow> foldValues(String barcode) {
		return valuesFromDb(barcode, "otgf.kct");
	}
	
	private List<ExpressionRow> valuesFromDb(String barcode, String dbFile) {
		DB db = null;
		B2RAffy.connect();
		String homePath = System.getProperty("otg.home");
		try {
			db = OTGQueries.open(homePath + "/" + dbFile);
			Map<String, ExprValue> r = OTGQueries.presentValuesByBarcode4J(db, barcode);
			System.out.println("Read " + r.size() + " records");
			List<ExpressionRow> rr = new ArrayList<ExpressionRow>();
			List<String> probeTitles = B2RAffy.titlesForJava(r.keySet().toArray(new String[0]));
			Iterator<String> ts = probeTitles.iterator();
			for (String probe: r.keySet()) {
				ExprValue ev = r.get(probe);
				ExpressionValue jev = new ExpressionValue(ev.value(), ev.call());
				if (ts.hasNext()) {
					rr.add(new ExpressionRow(probe, ts.next(), jev));
				} else {
					rr.add(new ExpressionRow(probe, "(none)", jev));
				}
			}
			System.out.println("Returning " + r.size() + " data rows");
			return rr;
		} finally {
			if (db != null) {
				System.out.println("DB closed");
				db.close();
			}
			B2RAffy.close();
		}
	}
	
	public int loadDataset(List<String> barcodes, String[] probes, ValueType type) {
		HttpServletRequest request = getThreadLocalRequest();
		HttpSession session = request.getSession();
		DB db = null;
		String homePath = System.getProperty("otg.home");
		String[] realProbes = probes;
		
		if (probes == null) {
			//get all probes
			realProbes = OTGQueries.probes(homePath + "/rat.probes.txt");
		} else {
			realProbes = OTGQueries.filterProbes(probes, homePath + "/rat.probes.txt");
		}
		System.out.println(realProbes.length + " probes requested after filtering");
		
		try {
			
			switch(type) {
			case Folds:
				db = OTGQueries.open(homePath + "/otgf.kct");
				break;
			case Absolute:
				db = OTGQueries.open(homePath + "/otg.kct");
				break;
			}
				
			ExprValue[][] r = OTGQueries.presentValuesByBarcodesAndProbes4J(db, barcodes, realProbes);
			
			session.setAttribute("dataset", r);
			session.setAttribute("datasetProbes", realProbes);
			if (r.length > 0) {
				System.out.println("Stored " + r.length + " x " + r[0].length + " items in session");
			} else {
				System.out.println("Stored empty data in session");
			}
			return r.length;
		}
		finally {
			if (db != null) {
				System.out.println("DB closed");
				db.close();
			}
		}		
	}
	
	public List<ExpressionRow> datasetItems(int offset, int size) {
		HttpServletRequest request = getThreadLocalRequest();
		HttpSession session = request.getSession();
		ExprValue[][] data = (ExprValue[][]) session.getAttribute("dataset");
		if (data != null) {
			System.out.println("I had " + (data).length + " rows stored");
		}
		String[] probes = (String[]) session.getAttribute("datasetProbes");
		List<ExpressionRow> r = new ArrayList<ExpressionRow>();

		if (probes != null && data != null) {
			try {
				B2RAffy.connect();

				
				List<String> probeTitles = B2RAffy.titlesForJava((String[]) Arrays.copyOfRange(probes, offset, offset+size));
				for (int i = offset; i < offset + size && i < probes.length; ++i) {
					ExpressionValue[] vals = new ExpressionValue[data[i].length];
					
					for (int j = 0; j < vals.length; ++j) {
						vals[j] = new ExpressionValue(data[i][j].value(), data[i][j].call());						
					}
					r.add(new ExpressionRow(probes[i], probeTitles.get(i
							- offset), vals));
				}
			} finally {
				B2RAffy.close();
			}
		}

		return r;
	}
}
