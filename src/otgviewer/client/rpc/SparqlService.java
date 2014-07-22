package otgviewer.client.rpc;
import javax.annotation.Nullable;

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

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * A service for obtaining data from SPARQL endpoints. These can be local or remote.
 * All methods in this service use their arguments to constrain the result that is being returned.
 * @author johan
 *
 */
@RemoteServiceRelativePath("sparql")
public interface SparqlService extends RemoteService {
	
	/**
	 * Obtain compounds for the given filter
	 * @param sc
	 * @return
	 */
	public String[] compounds(SampleClass sc);	
	
	/**
	 * Obtain dose levels for a given filter 
	 * @param sc
	 * @return
	 */
	public String[] doseLevels(SampleClass sc);
	
	/**
	 * Obtain samples for a given sample class.
	 * @param sc
	 * @return
	 */
	public Barcode[] samples(SampleClass sc);
	
	/**
	 * Obtain samples for a given sample class and a set of compounds.
	 * @param sc
	 * @return
	 */
	public Barcode[] samples(SampleClass sc, String[] compounds);

	/**
	 * Obtain all sample classes in the triple store
	 * @return
	 */
	public SampleClass[] sampleClasses();
	
	/**
	 * TODO consider removing
	 * 
	 * Obtain times corresponding to a data filter and a compound.
	 * @param filter 
	 * @param compound the chosen compound, or null for no constraint.
	 * @return
	 */
	public String[] times(SampleClass sc);		
	
	/**
	 * Obtain units that are populated with the samples that belong to them.
	 * @param sc
	 * @param compounds
	 * @return
	 */
	public BUnit[] units(SampleClass sc, @Nullable String[] compounds);
			
//	
//	/**
//	 * Obtain probes for the given barcodes
//	 * @param columns
//	 * @return
//	 */
//	public String[] probes(BarcodeColumn[] columns);
	
	/**
	 * Obtain pathologies for the given sample
	 * @param barcode
	 * @return
	 */
	public Pathology[] pathologies(Barcode barcode);
	
	/**
	 * Obtain pathologies for a set of samples
	 * @param column
	 * @return
	 */
	public Pathology[] pathologies(BarcodeColumn column);
	
	/**
	 * Annotations are experiment-associated information such as
	 * dose, time, biochemical data etc.
	 * This method obtains them for a single sample.
	 * @param barcode
	 * @return
	 */
	public Annotation annotations(Barcode barcode);
	
	/**
	 * Obtain annotations for a set of samples
	 * @param column
	 * @param importantOnly If true, a smaller set of core annotations will be obtained. If false,
	 * all annotations will be obtained.
	 * @return
	 */
	public Annotation[] annotations(HasSamples<Barcode> column, boolean importantOnly);
	

	/**
	 * Obtain pathway names matching the pattern (partial name)
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
	 *  (TODO it might be better to use an enum)
	 * @param homologous Whether to use homologous genes (if not, only direct targets are returned)
	 * @return
	 */
	public String[] probesTargetedByCompound(DataFilter filter, String compound, String service, 
			boolean homologous);
	
	/**
	 * Obtain GO terms matching the given pattern (partial name)
	 * @param pattern
	 * @return
	 */
	public String[] goTerms(String pattern);
	
	/**
	 * Obtain probes for a given GO term (fully named)
	 * @param filter
	 * @param goTerm
	 * @return
	 */
	public String[] probesForGoTerm(DataFilter filter, String goTerm);
	
	/**
	 * Obtain gene symbols for the given probes.
	 * The resulting array will contain gene symbol arrays in the same order as
	 * and corresponding to the probes in the input array.
	 * @param filter
	 * @param probes
	 * @return
	 */
	public String[][] geneSyms(DataFilter filter, String[] probes);
	
	/*
	 * Obtain gene suggestions from a partial gene symbol
	 * @param partialName
	 * @return An array of pairs, where the first item is the precise gene symbol and the second is the full gene name.
	 */
	public String[] geneSuggestions(DataFilter filter, String partialName);
	
	/**
	 * Obtain associations -- the "dynamic columns" on the data screen.
	 * @param types the association types to get.
	 * @param filter
	 * @param probes
	 * @return
	 */
	public Association[] associations(DataFilter filter, AType[] types, String[] probes);
}
