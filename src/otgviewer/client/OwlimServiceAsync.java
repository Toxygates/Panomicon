package otgviewer.client;

import otgviewer.shared.Barcode;
import otgviewer.shared.DataFilter;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface OwlimServiceAsync {

	public void compounds(DataFilter filter, AsyncCallback<String[]> callback);	
	public void organs(DataFilter filter, String compound, AsyncCallback<String[]> callback);	
	public void doseLevels(DataFilter filter, String compound, String organ, AsyncCallback<String[]> callback);	
	public void barcodes(DataFilter filter, String compound, String organ, 
			String doseLevel, String time, AsyncCallback<Barcode[]> callback);	
	public void times(DataFilter filter, String compound, String organ, AsyncCallback<String[]> callback);	
	public void probes(DataFilter filter, AsyncCallback<String[]> callback);
	
	public void pathways(DataFilter filter, String pattern, AsyncCallback<String[]> callback);
	public void probesForPathway(DataFilter filter, String pathway, AsyncCallback<String[]> callback);
	public void probesTargetedByCompound(DataFilter filter, String compound, String service, AsyncCallback<String[]> callback);
	
	public void goTerms(String pattern, AsyncCallback<String[]> callback);
	public void probesForGoTerm(DataFilter filter, String term, AsyncCallback<String[]> callback);
}
