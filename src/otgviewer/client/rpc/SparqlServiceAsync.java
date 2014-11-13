package otgviewer.client.rpc;

import otgviewer.shared.OTGColumn;
import otgviewer.shared.OTGSample;
import otgviewer.shared.Pathology;
import t.common.shared.DataSchema;
import t.common.shared.Pair;
import t.common.shared.SampleClass;
import t.common.shared.Unit;
import t.common.shared.sample.Annotation;
import t.common.shared.sample.HasSamples;
import t.viewer.shared.AType;
import t.viewer.shared.Association;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface SparqlServiceAsync {

	public void sampleClasses(AsyncCallback<SampleClass[]> callback);
	
	public void parameterValues(SampleClass sc, String parameter, 
			AsyncCallback<String[]> callback);
	public void parameterValues(SampleClass[] scs, String parameter, 
			AsyncCallback<String[]> callback);
		
	public void samples(SampleClass sc, AsyncCallback<OTGSample[]> callback);	
	public void samples(SampleClass sc, String param, String[] paramValues, 
			AsyncCallback<OTGSample[]> callback);
	public void samples(SampleClass[] scs, String param, String[] paramValues, 
			AsyncCallback<OTGSample[]> callback);
	
	public void units(SampleClass sc,
			String param, String[] paramValues, 
			AsyncCallback<Pair<Unit, Unit>[]> callback);
	
	public void units(SampleClass[] sc,
			String param, String[] paramValues, 
			AsyncCallback<Pair<Unit, Unit>[]> callback);

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
