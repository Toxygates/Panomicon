package otgviewer.client.rpc;

import otgviewer.shared.AType;
import otgviewer.shared.Association;
import otgviewer.shared.BUnit;
import otgviewer.shared.Barcode;
import otgviewer.shared.BarcodeColumn;
import otgviewer.shared.DataFilter;
import otgviewer.shared.Pathology;
import otgviewer.shared.SampleClass;
import bioweb.shared.array.Annotation;
import bioweb.shared.array.HasSamples;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface SparqlServiceAsync {

	public void compounds(DataFilter filter, AsyncCallback<String[]> callback);
		
	public void doseLevels(DataFilter filter, String compound, AsyncCallback<String[]> callback);	
	public void samples(DataFilter filter, String compound, 
			String doseLevel, String time, AsyncCallback<Barcode[]> callback);
	public void samples(DataFilter filter, String[] compounds, 
			String doseLevel, String time, AsyncCallback<Barcode[]> callback);
	public void units(DataFilter filter, String[] compounds, 
			String doseLevel, String time, AsyncCallback<BUnit[]> callback);
	
	public void sampleClasses(AsyncCallback<SampleClass[]> callback);
	
	public void times(DataFilter filter, String compound, AsyncCallback<String[]> callback);	
	public void probes(DataFilter filter, AsyncCallback<String[]> callback);
	
	public void pathologies(BarcodeColumn column, AsyncCallback<Pathology[]> callback);
	public void pathologies(Barcode barcode, AsyncCallback<Pathology[]> callback);
	
	public void annotations(HasSamples<Barcode> column, boolean importantOnly,
			AsyncCallback<Annotation[]> callback);
	public void annotations(Barcode barcode, AsyncCallback<Annotation> callback);
	
	public void pathways(DataFilter filter, String pattern, AsyncCallback<String[]> callback);
	public void probesForPathway(DataFilter filter, String pathway, AsyncCallback<String[]> callback);
	public void probesTargetedByCompound(DataFilter filter, String compound, String service, 
			boolean homologous, AsyncCallback<String[]> callback);
	
	public void geneSyms(DataFilter filter, String[] probes, AsyncCallback<String[][]> callback);
	public void geneSuggestions(DataFilter filter, String partialName, AsyncCallback<String[]> callback);
	
	public void goTerms(String pattern, AsyncCallback<String[]> callback);
	public void probesForGoTerm(DataFilter filter, String term, AsyncCallback<String[]> callback);
	
	public void associations(DataFilter filter, AType[] types, String[] probes, 
			AsyncCallback<Association[]> callback);
}
