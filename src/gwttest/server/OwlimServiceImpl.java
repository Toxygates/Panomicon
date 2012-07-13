package gwttest.server;

import java.util.List;
import otg.*;
import gwttest.client.OwlimService;
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

	public String[] barcodes(String compound, String organ, String doseLevel, String time) {
		try {
			OTGOwlim.connect();
			return OTGOwlim.barcodes(compound, organ, doseLevel, time);
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

}
