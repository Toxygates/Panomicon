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
import t.viewer.shared.Pathology;
import t.common.shared.Dataset;
import t.common.shared.Pair;
import t.common.shared.RequestResult;
import t.common.shared.sample.PrecomputedVarianceSet;
import t.common.shared.sample.Sample;
import t.common.shared.sample.Unit;
import t.common.shared.sample.search.MatchCondition;
import t.model.SampleClass;
import t.model.sample.Attribute;
import t.model.sample.SampleLike;
import t.viewer.shared.TimeoutException;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * A service that provides information about samples, datasets, and 
 * related objects.
 */
@RemoteServiceRelativePath("sample")
public interface SampleService extends RemoteService {
  
  /**
   * Choose the visible datasets. This changes session state on the server side. All methods in this
   * interface that obtain information about samples and their parameters respect this state, unless
   * otherwise stated.
   * 
   * @param enabled The enabled datasets
   * @return Sample classes in the new dataset view.
   * @throws TimeoutException
   */
  SampleClass[] chooseDatasets(String userKey, Dataset[] enabled) throws TimeoutException;

  Dataset[] datasetsForUser(String userKey) throws TimeoutException;

  /**
   * Obtain all values for a given parameter.
   * 
   * @param ds Select values across these datasets
   * @param sc Select values from samples in this sample class
   * @param parameter The parameter to select values for
   * @return
   * @throws TimeoutException
   */
  String[] parameterValues(Dataset[] ds, SampleClass sc, String parameter)
      throws TimeoutException;

  /**
   * Obtain all values for a given parameter, in the currently selected datasets in the session
   * state.
   * 
   * @param sc Select values from samples in this sample class
   * @param parameter The parameter to select values for
   * @return
   * @throws TimeoutException
   */
  String[] parameterValues(SampleClass sc, String parameter) throws TimeoutException;

  /**
   * Obtain samples, populated with the standard attributes, for the given ids. Keeps samples
   * grouped in the same way that the ids parameter was grouped upon calling this function.
   * 
   * @param ids
   * @return
   * @throws TimeoutException
   */
  List<Sample[]> samplesById(List<String[]> ids) throws TimeoutException;
  
  /**
   * Obtain samples for a given sample class, populated with the standard attributes.
   */
  Sample[] samples(SampleClass sc) throws TimeoutException;

  /**
   * Obtain all samples matching the constraints in a sample class
   * @param sc the sample class to select samples from
   * @param importantOnly if true, only attributes for the preview display
   *                      will be fetched. Otherwise, all attributes will be
   *                      fetched
   * @return
   */
  Sample[] samplesWithAttributes(SampleClass sc, boolean importantOnly);

  /**
   * Obtain samples for the given sample classes, with a filter on one parameter, populated with the
   * standard attributes.
   * 
   * @param scs Sample classes to select samples from
   * @param param parameter to filter
   * @param paramValues acceptable values for the parameter.
   */
  Sample[] samples(SampleClass[] scs, String param, String[] paramValues) throws TimeoutException;
  
  /**
   * Obtain units that are populated with the samples that belong to them, with a filter on one
   * parameter.
   * 
   * @param sc The sample class to select samples from
   * @param param The parameter to filter on
   * @param paramValues acceptable values for the parameter
   * @return Pairs of units, where the first is treated samples and the second the corresponding
   *         control samples, or null if there are none.
   */
  Pair<Unit, Unit>[] units(SampleClass sc, String param, @Nullable String[] paramValues)
      throws TimeoutException;

  /**
   * Obtain units that are populated with the samples that belong to them, with a filter on one
   * parameter.
   * 
   * @param scs The sample classes to select samples from
   * @param param The parameter to filter on
   * @param paramValues acceptable values for the parameter
   * @return Pairs of units, where the first is treated samples and the second the corresponding
   *         control samples, or null if there are none.
   */
  Pair<Unit, Unit>[] units(SampleClass[] scs, String param, @Nullable String[] paramValues)
      throws TimeoutException;

  /**
   * Find all attributes for which at least one of the samples matching the sample
   * class filter has a value.
   */
  Attribute[] attributesForSamples(SampleClass sc) throws TimeoutException;

  /**
   * Fetch parameter values for the given samples
   * @param samples the samples for which to fetch values
   * @param attributes the parameters to fetch
   * @return an array of Sample objects populated with attribute values
   * @throws TimeoutException
   */
  Sample[] parameterValuesForSamples(Sample[] samples, Attribute[] attributes) throws TimeoutException;

  /**
   * Fetch parameter values for the given samples, as well as variance information
   * @param samples the samples for which to fetch parameter values
   * @param importantOnly if true, only the parameters for the preview
   *                      display will be fetched. Otherwise, all attributes
   *                      will be fetched.
   * @return a Pair, where the first element is an array of samples with
   * attribute values, and the second element is a Map from sample IDs to
   * PrecomputedVarianceSets
   * @throws TimeoutException
   */
  Pair<Sample[], Map<String, PrecomputedVarianceSet>> attributeValuesAndVariance(Sample[] samples, boolean importantOnly)
    throws TimeoutException;

  /**
   * Prepare a CSV file with annotation information for download.
   *
   * @param samples The samples to include in the downloadable file.
   * @return The URL of the downloadable file.
   * @throws TimeoutException
   */
  String prepareAnnotationCSVDownload(Sample[] samples) throws TimeoutException;
  
  /**
   * Search for samples
   * 
   * @param sampleClass The class to search within
   * @param condition
   * @param maxResults
   * @return
   * @throws TimeoutException
   */
  RequestResult<Pair<Sample, Pair<Unit, Unit>>> sampleSearch(SampleClass sampleClass,
      MatchCondition condition, int maxResults) throws TimeoutException;

  /**
   * Search for units
   * 
   * @param sampleClass The class to search within
   * @param condition
   * @param maxResults
   * @return Treated and control unit pairs
   * @throws TimeoutException
   */
  RequestResult<Pair<Unit, Unit>> unitSearch(SampleClass sampleClass, MatchCondition condition,
      int maxResults)
      throws TimeoutException;

  /**
   * Prepare a CSV file with attribute information for download.
   * 
   * @param samples The samples to include in the download
   * @param attributes The attributes to include in the download
   * @return The URL of the downloadable file
   * @throws TimeoutException
   */
  String prepareCSVDownload(SampleLike[] samples, Attribute[] attributes) throws TimeoutException;

  /**
   * Obtain pathologies for a set of samples
   */
  Pathology[] pathologies(Sample[] samples) throws TimeoutException;
}
