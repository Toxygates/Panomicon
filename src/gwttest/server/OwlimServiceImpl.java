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
			return OTGOwlim.compounds();
		} finally {
			OTGOwlim.close();
		}
	}

	public String[] organs(String compound) {
		try {
			return OTGOwlim.organs(compound);
		} finally {
			OTGOwlim.close();
		}
	}

	public String[] doseLevels(String compound, String organ) {
		try {
			return OTGOwlim.doseLevels(compound, organ);
		} finally {
			OTGOwlim.close();
		}
	}

	public String[] barcodes(String compound, String organ, String doseLevel) {
		try {
			return OTGOwlim.barcodes(compound, organ, doseLevel);
		} finally {
			OTGOwlim.close();
		}
	}

}
