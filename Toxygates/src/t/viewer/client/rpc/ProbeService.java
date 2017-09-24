package t.viewer.client.rpc;

import java.util.List;

import javax.annotation.Nullable;

import t.common.shared.AType;
import t.common.shared.Pair;
import t.common.shared.sample.Sample;
import t.model.SampleClass;
import t.viewer.shared.TimeoutException;

import com.google.gwt.user.client.rpc.RemoteService;

/**
 * A service that provides information about probes and related objects
 */
public interface ProbeService extends RemoteService {
  
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
}
