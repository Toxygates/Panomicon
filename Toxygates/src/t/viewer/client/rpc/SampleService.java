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

import t.common.shared.*;
import t.common.shared.sample.*;
import t.common.shared.sample.search.MatchCondition;
import t.model.SampleClass;
import t.model.sample.Attribute;
import t.model.sample.SampleLike;
import t.viewer.shared.TimeoutException;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * A service that provides information about samples, datasets, and 
 * related objects.
 */

@RemoteServiceRelativePath("sample")
public interface SampleService extends RemoteService {
  
  /**
   * Choose the visible datasets.
   * 
   * @param enabled 
   * @return sample classes in the new dataset view.
   * @throws TimeoutException
   */
  SampleClass[] chooseDatasets(Dataset[] enabled) throws TimeoutException;
  
  String[] parameterValues(Dataset[] ds, SampleClass sc, String parameter)
      throws TimeoutException;

  String[] parameterValues(SampleClass sc, String parameter) throws TimeoutException;

  String[] parameterValues(SampleClass[] scs, String parameter) throws TimeoutException;
  
  /**
   * Obtain samples (fully populated with metadata) from given IDs
   * 
   * @param ids
   * @return
   * @throws TimeoutException
   */
  Sample[] samplesById(String[] ids) throws TimeoutException;

  /**
   * Obtain samples, fully populated with metadata, from given IDs.
   * Convenience function that keeps samples grouped.
   * @param ids
   * @return
   * @throws TimeoutException
   */
  List<Sample[]> samplesById(List<String[]> ids) throws TimeoutException;
  
  /**
   * Obtain samples for a given sample class.
   * 
   * @param sc
   * @return
   */
  Sample[] samples(SampleClass sc) throws TimeoutException;

  /**
   * Obtain samples with a filter on one parameter.
   * 
   * @param sc
   * @return
   */
  Sample[] samples(SampleClass sc, String param, String[] paramValues)
      throws TimeoutException;

  Sample[] samples(SampleClass[] scs, String param, String[] paramValues)
      throws TimeoutException;
  
  /**
   * Obtain units that are populated with the samples that belong to them, with a filter on one
   * parameter.
   * 
   * @param sc
   * @param
   * @return Pairs of units, where the first is treated samples and the second the corresponding
   *         control samples, or null if there are none.
   */
  Pair<Unit, Unit>[] units(SampleClass sc, String param, @Nullable String[] paramValues)
      throws TimeoutException;

  Pair<Unit, Unit>[] units(SampleClass[] scs, String param, @Nullable String[] paramValues)
      throws TimeoutException;


  /**
   * Annotations are experiment-associated information such as dose, time, biochemical data etc.
   * This method obtains them for a single sample.
   * 
   * @param barcode
   * @return
   */
  Annotation annotations(Sample barcode) throws TimeoutException;

  /**
   * Obtain annotations for a set of samples
   * 
   * @param samples
   * @param attributes the attributes to fetch
   * @return
   */
  Annotation[] annotations(Sample[] samples, Attribute[] attributes) throws TimeoutException;

  /**
   * Obtain annotations for a set of samples
   * 
   * @param column
   * @param importantOnly If true, a smaller set of core annotations will be obtained. If false, all
   *        annotations will be obtained.
   * @return
   */
  Annotation[] annotations(HasSamples<Sample> column, boolean importantOnly)
      throws TimeoutException;

  /**
   * Prepare a CSV file with annotation information for download.
   * @param column
   * @return The URL of the downloadable file.
   * @throws TimeoutException
   */
  String prepareAnnotationCSVDownload(HasSamples<Sample> column) throws TimeoutException;
  
  RequestResult<Pair<Sample, Pair<Unit, Unit>>> sampleSearch(SampleClass sampleClass,
      MatchCondition condition, int maxResults) throws TimeoutException;

  RequestResult<Pair<Unit, Unit>> unitSearch(SampleClass sampleClass, MatchCondition condition,
      int maxResults)
      throws TimeoutException;

  String prepareCSVDownload(SampleLike[] samples, Attribute[] attributes) throws TimeoutException;
}
