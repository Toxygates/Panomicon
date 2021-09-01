/*
 * Copyright (c) 2012-2019 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition (NIBIOHN), Japan.
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

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import t.shared.common.AType;
import t.shared.common.Pair;
import t.shared.common.sample.Sample;
import t.model.SampleClass;
import t.viewer.shared.AppInfo;
import t.viewer.shared.Association;
import t.viewer.shared.TimeoutException;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A service that provides information about probes and related objects
 */

@RemoteServiceRelativePath("probe")
public interface ProbeService extends RemoteService {

  /**
   * Obtain general application info. 
   * @return
   * @throws TimeoutException
   */
  AppInfo appInfo(@Nullable String userDataKey) throws TimeoutException;
  
  /**
   * Obtain gene symbols for the given probes. The resulting array will contain gene symbol arrays
   * in the same order as and corresponding to the probes in the input array.
   */
  String[][] geneSyms(String[] probes) throws TimeoutException;

  /**
   * Obtain gene suggestions from a partial gene symbol
   * 
   * @param partialName
   * @return An array of pairs, where the first item is the gene symbol and the second is the
   *         identifier
   */
  Pair<String, String>[] geneSuggestions(SampleClass sc, String partialName)
      throws TimeoutException;
  
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
   * Obtain suggestions from a partial gene symbol
   * 
   * @return An array of pairs, where the first item is the precise gene symbol and the second is
   *         the full gene name.
   */
  Pair<String, AType>[] keywordSuggestions(String partialName, int maxSize);

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
   * Obtain filtered probes for a given GO term (fully named)
   */
  String[] probesForGoTerm(String goTerm, @Nullable List<Sample> samples)
      throws TimeoutException;

  /**
   * Obtain associations -- the "dynamic columns" on the data screen.
   * 
   * @param types the association types to get.
   * @param probes
   * @param sizeLimit A limit on the number of associations to return for
   *                  each probe. The results in the data wil be truncated
   *                  based on this limit. Note: if >n associations are found
   *                  for a probe, then n+1 will be kept, in order to provide an
   *                  indication that >n results were found.
   * @return
   */
  Association[] associations(SampleClass sc, AType[] types, String[] probes,
      int sizeLimit) throws TimeoutException;

  /**
   * Obtain probes that correspond to proteins targeted by the named compound.
   *
   * @param service Service to use for lookup (currently DrugBank or CHEMBL)
   * (note: it might be better to use an enum)
   * @param homologous Whether to use homologous genes (if not, only direct targets are returned)
   */
  String[] probesTargetedByCompound(SampleClass sc, String compound, String service,
                                    boolean homologous) throws TimeoutException;
}
