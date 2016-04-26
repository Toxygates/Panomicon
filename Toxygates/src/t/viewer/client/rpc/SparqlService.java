/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health
 * and Nutrition (NIBIOHN), Japan.
 * 
 * This file is part of Toxygates.
 * 
 * Toxygates is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * Toxygates is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Toxygates. If not,
 * see <http://www.gnu.org/licenses/>.
 */

package t.viewer.client.rpc;

import java.util.List;

import javax.annotation.Nullable;

import otgviewer.shared.Pathology;
import otgviewer.shared.TimeoutException;
import t.common.shared.AType;
import t.common.shared.Dataset;
import t.common.shared.Pair;
import t.common.shared.SampleClass;
import t.common.shared.sample.Annotation;
import t.common.shared.sample.HasSamples;
import t.common.shared.sample.Sample;
import t.common.shared.sample.SampleColumn;
import t.common.shared.sample.Unit;
import t.viewer.shared.AppInfo;
import t.viewer.shared.Association;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * A service for obtaining data from SPARQL endpoints. These can be local or remote. All methods in
 * this service use their arguments to constrain the result that is being returned.
 * 
 * TODO this API is too big, try to make it smaller
 * 
 * @author johan
 * 
 */
@RemoteServiceRelativePath("sparql")
public interface SparqlService extends RemoteService {

  /**
   * Obtain general application info. 
   * TODO migrate one-time mandatory data retrival to this object
   * to make the API smaller and reduce the number of calls
   * 
   * @return
   * @throws TimeoutException
   */
  public AppInfo appInfo(@Nullable String userDataKey) throws TimeoutException;

  /**
   * Choose the subset of datasets to work with. This must be a subset of the datasets previously
   * returned by the datasets() call.
   * 
   * @param enabled
   * @throws TimeoutException
   */
  public void chooseDatasets(Dataset[] enabled) throws TimeoutException;

  public String[] parameterValues(Dataset[] ds, SampleClass sc, String parameter)
      throws TimeoutException;

  public String[] parameterValues(SampleClass sc, String parameter) throws TimeoutException;

  public String[] parameterValues(SampleClass[] scs, String parameter) throws TimeoutException;

  /**
   * Obtain samples (fully populated with metadata) from given IDs
   * 
   * @param ids
   * @return
   * @throws TimeoutException
   */
  public Sample[] samplesById(String[] ids) throws TimeoutException;

  /**
   * Obtain samples for a given sample class.
   * 
   * @param sc
   * @return
   */
  public Sample[] samples(SampleClass sc) throws TimeoutException;

  /**
   * Obtain samples with a filter on one parameter.
   * 
   * @param sc
   * @return
   */
  public Sample[] samples(SampleClass sc, String param, String[] paramValues)
      throws TimeoutException;

  public Sample[] samples(SampleClass[] scs, String param, String[] paramValues)
      throws TimeoutException;

  /**
   * Obtain all sample classes in the triple store
   * 
   * @return
   */
  @Deprecated
  public SampleClass[] sampleClasses() throws TimeoutException;

  /**
   * Obtain units that are populated with the samples that belong to them, with a filter on one
   * parameter.
   * 
   * @param sc
   * @param
   * @return Pairs of units, where the first is treated samples and the second the corresponding
   *         control samples, or null if there are none.
   */
  public Pair<Unit, Unit>[] units(SampleClass sc, String param, @Nullable String[] paramValues)
      throws TimeoutException;

  public Pair<Unit, Unit>[] units(SampleClass[] scs, String param, @Nullable String[] paramValues)
      throws TimeoutException;

  /**
   * Obtain pathologies for a set of samples
   * 
   * @param column
   * @return
   */
  public Pathology[] pathologies(SampleColumn column) throws TimeoutException;

  /**
   * Annotations are experiment-associated information such as dose, time, biochemical data etc.
   * This method obtains them for a single sample.
   * 
   * @param barcode
   * @return
   */
  public Annotation annotations(Sample barcode) throws TimeoutException;

  /**
   * Obtain annotations for a set of samples
   * 
   * @param column
   * @param importantOnly If true, a smaller set of core annotations will be obtained. If false, all
   *        annotations will be obtained.
   * @return
   */
  public Annotation[] annotations(HasSamples<Sample> column, boolean importantOnly)
      throws TimeoutException;

  /**
   * Obtain pathway names matching the pattern (partial name)
   * 
   * @param pattern
   * @return
   */
  public String[] pathways(SampleClass sc, String pattern) throws TimeoutException;

  /**
   * Obtain filtered probes that belong to the named pathway.
   * 
   * @param pathway
   * @param samples If null, all probes will be obtained.
   * @return
   */
  public String[] probesForPathway(SampleClass sc, String pathway, @Nullable List<Sample> samples)
      throws TimeoutException;

  /**
   * Obtain probes that correspond to proteins targeted by the named compound.
   * 
   * @param compound
   * @param service Service to use for lookup (currently DrugBank or CHEMBL) (TODO it might be
   *        better to use an enum)
   * @param homologous Whether to use homologous genes (if not, only direct targets are returned)
   * @return
   */
  public String[] probesTargetedByCompound(SampleClass sc, String compound, String service,
      boolean homologous) throws TimeoutException;

  /**
   * Obtain GO terms matching the given pattern (partial name)
   * 
   * @param pattern
   * @return
   */
  public String[] goTerms(String pattern) throws TimeoutException;

  /**
   * Obtain probes for a given GO term (fully named)
   * 
   * @param goTerm
   * @return
   */
  public String[] probesForGoTerm(String goTerm) throws TimeoutException;

  /**
   * Obtain filtered probes for a given GO term (fully named)
   * 
   * @param goTerm
   * @return
   */
  public String[] probesForGoTerm(String goTerm, @Nullable List<Sample> samples)
      throws TimeoutException;

  /**
   * Obtain gene symbols for the given probes. The resulting array will contain gene symbol arrays
   * in the same order as and corresponding to the probes in the input array.
   * 
   * @param probes
   * @return
   */
  public String[][] geneSyms(String[] probes) throws TimeoutException;

  /*
   * Obtain gene suggestions from a partial gene symbol
   * 
   * @param partialName
   * 
   * @return An array of pairs, where the first item is the precise gene symbol and the second is
   * the full gene name.
   */
  public String[] geneSuggestions(SampleClass sc, String partialName) throws TimeoutException;

  /**
   * Obtain associations -- the "dynamic columns" on the data screen.
   * 
   * @param types the association types to get.
   * @param filter
   * @param probes
   * @return
   */
  public Association[] associations(SampleClass sc, AType[] types, String[] probes)
      throws TimeoutException;

  /**
   * Filter probes by given samples
   * 
   * @param probes
   * @param samples
   * @return
   */
  public String[] filterProbesByGroup(String[] probes, List<Sample> samples);

  /**
   * Obtain suggestions from a partial gene symbol
   * 
   * @param partialName
   * 
   * @return An array of pairs, where the first item is the precise gene symbol and the second is
   *         the full gene name.
   */
  public Pair<String, AType>[] keywordSuggestions(String partialName, int maxSize);
}
