package otgviewer.client;

import otgviewer.shared.AType;
import otgviewer.shared.Annotation;
import otgviewer.shared.Association;
import otgviewer.shared.Barcode;
import otgviewer.shared.DataColumn;
import otgviewer.shared.DataFilter;
import otgviewer.shared.MatchResult;
import otgviewer.shared.Pair;
import otgviewer.shared.Pathology;
import otgviewer.shared.RankRule;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface SparqlServiceAsync {

	public void compounds(DataFilter filter, AsyncCallback<String[]> callback);
	
	public void organs(DataFilter filter, String compound, AsyncCallback<String[]> callback);	
	public void doseLevels(DataFilter filter, String compound, AsyncCallback<String[]> callback);	
	public void barcodes(DataFilter filter, String compound, 
			String doseLevel, String time, AsyncCallback<Barcode[]> callback);
	public void barcodes(DataFilter filter, String[] compounds, 
			String doseLevel, String time, AsyncCallback<Barcode[]> callback);
	
	public void times(DataFilter filter, String compound, AsyncCallback<String[]> callback);	
	public void probes(DataFilter filter, AsyncCallback<String[]> callback);
	
	public void pathologies(DataColumn column, AsyncCallback<Pathology[]> callback);
	public void pathologies(Barcode barcode, AsyncCallback<Pathology[]> callback);
	
	public void annotations(DataColumn column, AsyncCallback<Annotation[]> callback);
	public void annotations(Barcode barcode, AsyncCallback<Annotation> callback);
	
	public void pathways(DataFilter filter, String pattern, AsyncCallback<String[]> callback);
	public void probesForPathway(DataFilter filter, String pathway, AsyncCallback<String[]> callback);
	public void probesTargetedByCompound(DataFilter filter, String compound, String service, 
			boolean homologous, AsyncCallback<String[]> callback);
	
	public void geneSyms(String[] probes, DataFilter filter, AsyncCallback<String[][]> callback);
	public void geneSuggestions(String partialName, DataFilter filter, AsyncCallback<Pair<String, String>[]> callback);
	
	public void goTerms(String pattern, AsyncCallback<String[]> callback);
	public void probesForGoTerm(DataFilter filter, String term, AsyncCallback<String[]> callback);
	
	public void associations(DataFilter filter, AType[] types, String[] probes, String[] geneIds, AsyncCallback<Association[]> callback);
}
