package gwttest.server;

import gwttest.client.ExpressionRow;
import gwttest.client.KCService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import kyotocabinet.DB;
import otg.OTGQuery;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class KCServiceImpl extends RemoteServiceServlet implements KCService {

	public List<ExpressionRow> absoluteValues(String barcode) {
		DB db = null;
		try {
			db = OTGQuery.open("/Users/johan/otg/20120221/open-tggates/otg.kct");
			Map<String, Double> r = OTGQuery.presentValuesByBarcodeForJava(db, barcode);
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
