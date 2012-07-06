package gwttest.client;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("owlim")
public interface OwlimService extends RemoteService {

	public String[] compounds();
	public String[] organs(String compound);
	public String[] doseLevels(String compound, String organ);	
	public String[] barcodes(String compound, String organ, String doseLevel);
}
