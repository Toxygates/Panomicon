package otgviewer.client.rpc;

import otgviewer.shared.AType;
import otgviewer.shared.Association;
import otgviewer.shared.BUnit;
import otgviewer.shared.Barcode;
import otgviewer.shared.BarcodeColumn;
import otgviewer.shared.DataFilter;
import otgviewer.shared.Pathology;
import t.common.shared.sample.Annotation;
import t.common.shared.sample.HasSamples;
import t.viewer.shared.SampleClass;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface SparqlServiceAsync {

	public void compounds(SampleClass sc, AsyncCallback<String[]> callback);
		
	public void doseLevels(SampleClass sc, AsyncCallback<String[]> callback);	
	public void samples(SampleClass sc, AsyncCallback<Barcode[]> callback);	
	public void samples(SampleClass sc, String[] compounds, 
			AsyncCallback<Barcode[]> callback);
	public void units(SampleClass sc, String[] compounds, 
			AsyncCallback<BUnit[]> callback);
	
	public void sampleClasses(AsyncCallback<SampleClass[]> callback);
	
	public void times(SampleClass sc, AsyncCallback<String[]> callback);	
//	public void probes(BarcodeColumn[] columns, AsyncCallback<String[]> callback);
	
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
