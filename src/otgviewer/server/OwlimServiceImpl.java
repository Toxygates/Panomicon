package otgviewer.server;

import otg.B2RAffy;
import otg.B2RKegg;
import otg.OTGOwlim;
import otg.OTGQueries;
import otgviewer.client.OwlimService;
import otgviewer.shared.Barcode;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class OwlimServiceImpl extends RemoteServiceServlet implements
		OwlimService {

	//Future: keep connection open, close on shutdown.
	
	@Override
	public String[] compounds() {
		try {
			OTGOwlim.connect();
			return OTGOwlim.compounds();
		} finally {
			OTGOwlim.close();
		}
	}

	public String[] organs(String compound) {
		try {
			OTGOwlim.connect();
			return OTGOwlim.organs(compound);
		} finally {
			OTGOwlim.close();
		}
	}

	public String[] doseLevels(String compound, String organ) {
		try {
			OTGOwlim.connect();
			return OTGOwlim.doseLevels(compound, organ);
		} finally {
			OTGOwlim.close();
		}
	}

	public Barcode[] barcodes(String compound, String organ, String doseLevel, String time) {
		try {
			OTGOwlim.connect();
			String[] codes = OTGOwlim.barcodes(compound, organ, doseLevel, time);
			Barcode[] r = new Barcode[codes.length];
			int i = 0;
			for (String code: codes) {
				r[i] = new Barcode(code, OTGOwlim.individual(code), OTGOwlim.dose(code),
						OTGOwlim.time(code));
				i += 1;
			}
			return r;
		} finally {
			OTGOwlim.close();
		}
	}
	
	public String[] times(String compound, String organ) {
		try {
			OTGOwlim.connect();
			return OTGOwlim.times(compound, organ);
		} finally {
			OTGOwlim.close();
		}
	}
	
	public String probeTitle(String probe) {
		try {
			B2RAffy.connect();
			return B2RAffy.title(probe);			
		} finally {
			B2RAffy.close();
		}
	}
	
	public String[] probes() {
		String homePath = System.getProperty("otg.home");
		return OTGQueries.probes(homePath + "/rat.probes.txt");
	}
	
	public String[] pathways(String pattern) {
		try {
			B2RKegg.connect();
			return B2RKegg.pathways(pattern, "rno");
		} finally {
			B2RKegg.close();
		}		
	}
	
	public String[] probes(String pathway) {
		try {
			B2RKegg.connect();
			OTGOwlim.connect();
			String homePath = System.getProperty("otg.home");
			String[] geneIds = B2RKegg.geneIds(pathway, "rno");
			System.out.println("Probes for " + geneIds.length + " genes");
			String [] probes = OTGOwlim.probes(geneIds);
			return OTGQueries.filterProbes(probes, homePath + "/rat.probes.txt");			
		} finally {
			B2RKegg.close();
			OTGOwlim.close();
		}		
	}

}
