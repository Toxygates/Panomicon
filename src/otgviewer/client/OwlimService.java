package otgviewer.client;
import otgviewer.shared.Barcode;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("owlim")
public interface OwlimService extends RemoteService {

	//Methods relating to metadata about our microarrays.
	public String[] probes();
	public String[] compounds();
	public String[] organs(String compound);
	public String[] doseLevels(String compound, String organ);	
	public Barcode[] barcodes(String compound, String organ, String doseLevel, String time);	
	public String[] times(String compound, String organ);		
	
	//Other methods.
	/**
	 * Obtain pathway names matching the pattern.
	 * @param pattern
	 * @return
	 */
	public String[] pathways(String pattern);
	
	/**
	 * Obtain probes that belong to the named pathway.
	 * @param pathway
	 * @return
	 */
	public String[] probes(String pathway);
}
