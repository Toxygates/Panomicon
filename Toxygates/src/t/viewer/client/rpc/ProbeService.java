package t.viewer.client.rpc;

import java.util.List;

import javax.annotation.Nullable;

import t.common.shared.AType;
import t.common.shared.Pair;
import t.common.shared.sample.Sample;
import t.model.SampleClass;
import t.viewer.shared.*;
import t.viewer.shared.mirna.MirnaSource;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * A service that provides information about probes and related objects
 */

@RemoteServiceRelativePath("probe")
public interface ProbeService extends RemoteService {

  /**
   * Obtain general application info. 
   * TODO migrate one-time mandatory data retrieval to this object
   * to make the API smaller and reduce the number of calls
   * 
   * @return
   * @throws TimeoutException
   */
  
  AppInfo appInfo(@Nullable String userDataKey) throws TimeoutException;
  /**
   * Obtain gene symbols for the given probes. The resulting array will contain gene symbol arrays
   * in the same order as and corresponding to the probes in the input array.
   * 
   * @param probes
   * @return
   */
  String[][] geneSyms(String[] probes) throws TimeoutException;

  /**
   * Obtain gene suggestions from a partial gene symbol
   * 
   * @param partialName
   * 
   * @return An array of pairs, where the first item is the precise gene symbol and the second is
   * the full gene name.
   */
  String[] geneSuggestions(SampleClass sc, String partialName) throws TimeoutException;
  
  /**
   * Convert identifiers such as genes, probe IDs and proteins into a list of probes.
   * 
   * @param filter
   * @param identifiers
   * @param precise If true, names must be an exact match, otherwise partial name matching is used.
   * @param titlePatternMatch If true, the query is assumed to be a partial pattern match on probe
   *        titles.
   * @param samples If null, all probes will be obtained.
   * @return
   */
  String[] identifiersToProbes(String[] identifiers, boolean precise,
      boolean quick, boolean titlePatternMatch, @Nullable List<Sample> samples);

  /**
   * Filter probes by given samples
   * 
   * @param probes
   * @param samples
   * @return
   */
  String[] filterProbesByGroup(String[] probes, List<Sample> samples);

  /**
   * Obtain suggestions from a partial gene symbol
   * 
   * @param partialName
   * 
   * @return An array of pairs, where the first item is the precise gene symbol and the second is
   *         the full gene name.
   */
  Pair<String, AType>[] keywordSuggestions(String partialName, int maxSize);
  

  
  /**
   * Obtain pathway names matching the pattern (partial name)
   * 
   * @param pattern
   * @return
   */
  String[] pathways(String pattern) throws TimeoutException;

  /**
   * Obtain filtered probes that belong to the named pathway.
   * 
   * @param pathway
   * @param samples If null, all probes will be obtained.
   * @return
   */
  String[] probesForPathway(String pathway, @Nullable List<Sample> samples)
      throws TimeoutException;

  /**
   * Obtain GO terms matching the given pattern (partial name)
   * 
   * @param pattern
   * @return
   */
  String[] goTerms(String pattern) throws TimeoutException;

  /**
   * Obtain probes for a given GO term (fully named)
   * 
   * @param goTerm
   * @return
   */
  String[] probesForGoTerm(String goTerm) throws TimeoutException;

  /**
   * Obtain filtered probes for a given GO term (fully named)
   * 
   * @param goTerm
   * @return
   */
  String[] probesForGoTerm(String goTerm, @Nullable List<Sample> samples)
      throws TimeoutException;

  /**
   * Obtain associations -- the "dynamic columns" on the data screen.
   * 
   * @param types the association types to get.
   * @param filter
   * @param probes
   * @return
   */
  Association[] associations(SampleClass sc, AType[] types, String[] probes)
      throws TimeoutException;
}
