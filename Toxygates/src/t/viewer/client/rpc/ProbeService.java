/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
 * (NIBIOHN), Japan.
 *
 * This file is part of Toxygates.
 *
 * Toxygates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Toxygates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Toxygates. If not, see <http://www.gnu.org/licenses/>.
 */

package t.viewer.client.rpc;

import java.util.List;

import javax.annotation.Nullable;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import t.common.shared.AType;
import t.common.shared.Pair;
import t.common.shared.sample.Sample;
import t.model.SampleClass;
import t.viewer.shared.*;
import t.viewer.shared.mirna.MirnaSource;

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
  
  /**
   * Set the desired miRNA association sources. In the objects passed in,
   * only the ID string and the score threshold (lower bound) are used.
   * The choices are persisted in the user's server side session.
   * @param sources
   * @throws TimeoutException
   */
  void setMirnaSources(MirnaSource[] sources) throws TimeoutException;
}
