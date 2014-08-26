package otgviewer.client.rpc;
import javax.annotation.Nullable;

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
	public String[] parameterValues(SampleClass sc, String parameter);
	
	/**
	 * Obtain samples for a given sample class.
	 * @param sc
	 * @return
	 */
	public OTGSample[] samples(SampleClass sc);
	
	/**
	 * Obtain samples with a filter on one parameter.
	 * @param sc
	 * @return
	 */
	public OTGSample[] samples(SampleClass sc, String param, String[] paramValues);

	/**
	 * Obtain all sample classes in the triple store
	 * @return
	 */
	public SampleClass[] sampleClasses();
	
	/**
	 * Obtain units that are populated with the samples that belong to them,
	 * with a filter on one parameter.
	 * @param sc
	 * @param 
	 * @return Pairs of units, where the first is treated samples and the second
	 * the corresponding control samples, or null if there are none.
	 */
	public Pair<Unit, Unit>[] units(SampleClass sc, DataSchema schema,
			String param, @Nullable String[] paramValues);
			
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
	public Pathology[] pathologies(OTGSample barcode);
	
	/**
	 * Obtain pathologies for a set of samples
	 * @param column
	 * @return
	 */
	public Pathology[] pathologies(OTGColumn column);
	
	/**
	 * Annotations are experiment-associated information such as
	 * dose, time, biochemical data etc.
	 * This method obtains them for a single sample.
	 * @param barcode
	 * @return
	 */
	public Annotation annotations(OTGSample barcode);
	
	/**
	 * Obtain annotations for a set of samples
	 * @param column
	 * @param importantOnly If true, a smaller set of core annotations will be obtained. If false,
	 * all annotations will be obtained.
	 * @return
	 */
	public Annotation[] annotations(HasSamples<OTGSample> column, boolean importantOnly);
	

	/**
	 * Obtain pathway names matching the pattern (partial name)
	 * @param pattern
	 * @return
	 */
	public String[] pathways(SampleClass sc, String pattern);
	
	/**
	 * Obtain probes that belong to the named pathway.
	 * @param pathway
	 * @return
	 */
	public String[] probesForPathway(SampleClass sc, String pathway);
	
	/**
	 * Obtain probes that correspond to proteins targeted by
	 * the named compound.
	 * @param compound
	 * @param service Service to use for lookup (currently DrugBank or CHEMBL)
	 *  (TODO it might be better to use an enum)
	 * @param homologous Whether to use homologous genes (if not, only direct targets are returned)
	 * @return
	 */
	public String[] probesTargetedByCompound(SampleClass sc, String compound, String service, 
			boolean homologous);
	
	/**
	 * Obtain GO terms matching the given pattern (partial name)
	 * @param pattern
	 * @return
	 */
	public String[] goTerms(String pattern);
	
	/**
	 * Obtain probes for a given GO term (fully named)
	 * @param goTerm
	 * @return
	 */
	public String[] probesForGoTerm(String goTerm);
	
	/**
	 * Obtain gene symbols for the given probes.
	 * The resulting array will contain gene symbol arrays in the same order as
	 * and corresponding to the probes in the input array.
	 * @param probes
	 * @return
	 */
	public String[][] geneSyms(String[] probes);
	
	/*
	 * Obtain gene suggestions from a partial gene symbol
	 * @param partialName
	 * @return An array of pairs, where the first item is the precise gene symbol and the second is the full gene name.
	 */
	public String[] geneSuggestions(SampleClass sc, String partialName);
	
	/**
	 * Obtain associations -- the "dynamic columns" on the data screen.
	 * @param types the association types to get.
	 * @param filter
	 * @param probes
	 * @return
	 */
	public Association[] associations(SampleClass sc, AType[] types, String[] probes);
}
