package gwttest.server;

import java.util.List;
import otg.*;
import gwttest.client.OwlimService;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class OwlimServiceImpl extends RemoteServiceServlet implements
		OwlimService {

	@Override
	public String[] compounds() {
		return OTGOwlim.compounds();
	}
	
	public String[] organs(String compound) {
		return OTGOwlim.organs(compound);
	}
	
	public String[] doseLevels(String compound, String organ) {
		return OTGOwlim.doseLevels(compound, organ);
	}

}
