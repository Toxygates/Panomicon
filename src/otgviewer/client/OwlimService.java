package otgviewer.client;
import otgviewer.shared.Annotation;
import otgviewer.shared.Association;
import otgviewer.shared.Barcode;
import otgviewer.shared.DataColumn;
import otgviewer.shared.DataFilter;
import otgviewer.shared.NoSuchProbeException;
import otgviewer.shared.Pair;
import otgviewer.shared.Pathology;
import otgviewer.shared.RankRule;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("owlim")
public interface OwlimService extends RemoteService {
	
	//Methods relating to metadata about our microarrays.
	public String[] compounds(DataFilter filter);
	public Pair<String, Double>[] rankedCompounds(DataFilter filter, RankRule[] rules) throws NoSuchProbeException;
	
	public String[] organs(DataFilter filter, String compound);
	public String[] doseLevels(DataFilter filter, String compound);	
	public Barcode[] barcodes(DataFilter filter, String compound, String doseLevel, String time);
	
	/**
	 * Obtain times corresponding to a data filter and a compound.
	 * Compound may be null, in which case all times for the filter are returned.
	 * @param filter
	 * @param compound
	 * @return
	 */
	public String[] times(DataFilter filter, String compound);		
		
	public String[] probes(DataFilter filter);
	
	public Pathology[] pathologies(Barcode barcode);	
	public Pathology[] pathologies(DataColumn column);
	
	public Annotation annotations(Barcode barcode);
	public Annotation[] annotations(DataColumn column);
	
	//Other methods.
	/**
	 * Obtain pathway names matching the pattern.
	 * @param pattern
	 * @return
	 */
	public String[] pathways(DataFilter filter, String pattern);
	
	/**
	 * Obtain probes that belong to the named pathway.
	 * @param pathway
	 * @return
	 */
	public String[] probesForPathway(DataFilter filter, String pathway);
	
	/**
	 * Obtain probes that correspond to proteins targeted by
	 * the named compound.
	 * @param compound
	 * @param service Service to use for lookup (currently DrugBank or CHEMBL)
	 * @param homologous Whether to use homologous genes (if not, only direct targets are returned)
	 * @return
	 */
	public String[] probesTargetedByCompound(DataFilter filter, String compound, String service, 
			boolean homologous);
	
	public String[] goTerms(String pattern);
	
	public String[] probesForGoTerm(DataFilter filter, String goTerm);
	
	public String[][] geneSyms(String[] probes);
	
	/**
	 * Obtain gene suggestions from a partial gene name (natural language)
	 * @param partialName
	 * @return An array of pairs, where the first item is the precise gene symbol and the second is the full gene name.
	 */
	public Pair<String, String>[] geneSuggestions(String partialName, DataFilter filter);
	
	public Association[] associations(DataFilter filter, String[] probes, String[] geneIds);
}
