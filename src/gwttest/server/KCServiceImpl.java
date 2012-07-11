package gwttest.server;

import gwttest.client.ExpressionRow;
import gwttest.client.KCService;
import gwttest.shared.ValueType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import kyotocabinet.DB;
import otg.B2RAffy;
import otg.OTGQueries;

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
			Map<String, Double> r = OTGQueries.presentValuesByBarcode4J(db, barcode);
			System.out.println("Read " + r.size() + " records");
			List<ExpressionRow> rr = new ArrayList<ExpressionRow>();
			List<String> probeTitles = B2RAffy.titlesForJava(r.keySet());
			Iterator<String> ts = probeTitles.iterator();
			for (String probe: r.keySet()) {
				if (ts.hasNext()) {
					rr.add(new ExpressionRow(probe, ts.next(), r.get(probe)));
				} else {
					rr.add(new ExpressionRow(probe, "(none)", r.get(probe)));
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
	
	public void loadDataset(List<String> barcodes, List<String> probes, ValueType type) {
		HttpServletRequest request = getThreadLocalRequest();
		HttpSession session = request.getSession();
		DB db = null;
		String homePath = System.getProperty("otg.home");
		try {
			
			switch(type) {
			case Folds:
				db = OTGQueries.open(homePath + "/otgf.kct");
				break;
			case Absolute:
				db = OTGQueries.open(homePath + "/otg.kct");
				break;
			}
				
			Double[][] r = OTGQueries.presentValuesByBarcodesAndProbes4J(db, barcodes, OTGQueries.nullToNone(probes));
			
			Double[][] oldData = (Double[][]) session.getAttribute("dataset");
			if (oldData != null) {
				System.out.println("I had " + (oldData).length + " rows stored");
			}
			
			session.setAttribute("dataset", r);
			session.setAttribute("datasetProbes", probes);
			System.out.println("Stored " + r.length + " x " + r[0].length + " items in session");
		}
		finally {
			if (db != null) {
				System.out.println("DB closed");
				db.close();
			}
		}
		
		System.out.println("Storing data in session");
	}
	
	public List<ExpressionRow> datasetItems(int offset, int size) {
		HttpServletRequest request = getThreadLocalRequest();
		HttpSession session = request.getSession();
		return new ArrayList<ExpressionRow>();
	}
}
