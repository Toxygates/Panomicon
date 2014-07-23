package otgviewer.client.rpc;

import otgviewer.shared.AType;
import otgviewer.shared.Association;
import otgviewer.shared.BUnit;
import otgviewer.shared.OTGSample;
import otgviewer.shared.OTGColumn;
import otgviewer.shared.DataFilter;
import otgviewer.shared.Pathology;
import t.common.shared.sample.Annotation;
import t.common.shared.sample.HasSamples;
import t.viewer.shared.SampleClass;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface SparqlServiceAsync {

	public void compounds(SampleClass sc, AsyncCallback<String[]> callback);
		
	public void doseLevels(SampleClass sc, AsyncCallback<String[]> callback);	
	public void samples(SampleClass sc, AsyncCallback<OTGSample[]> callback);	
	public void samples(SampleClass sc, String[] compounds, 
			AsyncCallback<OTGSample[]> callback);
	public void units(SampleClass sc, String[] compounds, 
			AsyncCallback<BUnit[]> callback);
	
	public void sampleClasses(AsyncCallback<SampleClass[]> callback);
	
	public void times(SampleClass sc, AsyncCallback<String[]> callback);	
//	public void probes(BarcodeColumn[] columns, AsyncCallback<String[]> callback);
	
	public void pathologies(OTGColumn column, AsyncCallback<Pathology[]> callback);
	public void pathologies(OTGSample barcode, AsyncCallback<Pathology[]> callback);
	
	public void annotations(HasSamples<OTGSample> column, boolean importantOnly,
			AsyncCallback<Annotation[]> callback);
	public void annotations(OTGSample barcode, AsyncCallback<Annotation> callback);
	
	public void pathways(SampleClass sc, String pattern, AsyncCallback<String[]> callback);
	public void probesForPathway(SampleClass sc, String pathway, AsyncCallback<String[]> callback);
	public void probesTargetedByCompound(SampleClass sc, String compound, String service, 
			boolean homologous, AsyncCallback<String[]> callback);
	
	public void geneSyms(String[] probes, AsyncCallback<String[][]> callback);
	public void geneSuggestions(SampleClass sc, String partialName, AsyncCallback<String[]> callback);
	
	public void goTerms(String pattern, AsyncCallback<String[]> callback);
	public void probesForGoTerm(String term, AsyncCallback<String[]> callback);
	
	public void associations(SampleClass sc, AType[] types, String[] probes, 
			AsyncCallback<Association[]> callback);
}
