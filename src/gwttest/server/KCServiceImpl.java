package gwttest.server;

import gwttest.client.ExpressionRow;
import gwttest.client.KCService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import kyotocabinet.DB;
import otg.OTGQueries;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class KCServiceImpl extends RemoteServiceServlet implements KCService {

	//Future: keep connection open, close on shutdown.
	
	public List<ExpressionRow> absoluteValues(String barcode) {
		DB db = null;
		String homePath = System.getProperty("otg.home");
		try {
			db = OTGQueries.open(homePath + "/otg.kct");
			Map<String, Double> r = OTGQueries.presentValuesByBarcodeForJava(db, barcode);
			List<ExpressionRow> rr = new ArrayList<ExpressionRow>();
			for (String probe: r.keySet()) {
				rr.add(new ExpressionRow(probe, r.get(probe)));	
			}
			System.out.println("Returning " + r.size() + " data rows");
			return rr;
		} finally {
			if (db != null) {
				System.out.println("DB closed");
				db.close();
			}
		}
	}
	
	public Map<String, Double> foldValues(String barcode) {
		return null;
	}
}
