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

import java.util.List;

import t.common.shared.*;
import t.common.shared.sample.*;
import t.common.shared.sample.search.MatchCondition;
import t.model.SampleClass;
import t.model.sample.Attribute;
import t.model.sample.SampleLike;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface SampleServiceAsync {

  void chooseDatasets(Dataset[] enabled, AsyncCallback<SampleClass[]> callback);

  void parameterValues(Dataset[] ds, SampleClass sc, String parameter,
      AsyncCallback<String[]> callback);

  void parameterValues(SampleClass sc, String parameter, AsyncCallback<String[]> callback);

  void samplesById(List<String[]> ids, AsyncCallback<List<Sample[]>> callback);

  void samples(SampleClass sc, AsyncCallback<Sample[]> callback);

  void samples(SampleClass[] scs, String param, String[] paramValues,
      AsyncCallback<Sample[]> callback);

  void units(SampleClass sc, String param, String[] paramValues,
      AsyncCallback<Pair<Unit, Unit>[]> callback);

  void units(SampleClass[] sc, String param, String[] paramValues,
      AsyncCallback<Pair<Unit, Unit>[]> callback);

  void annotations(Sample sample, AsyncCallback<Annotation> callback);

  void annotations(Sample[] samples, Attribute[] attributes, AsyncCallback<Annotation[]> callback);

  void annotations(HasSamples<Sample> column, boolean importantOnly,
      AsyncCallback<Annotation[]> callback);
  
  void prepareAnnotationCSVDownload(HasSamples<Sample> column, 
      AsyncCallback<String> callback);

  void sampleSearch(SampleClass sampleClass, MatchCondition condition, int maxResults,
      AsyncCallback<RequestResult<Pair<Sample, Pair<Unit, Unit>>>> callback);

  void unitSearch(SampleClass sampleClass, MatchCondition condition, int maxResults,
      AsyncCallback<RequestResult<Pair<Unit, Unit>>> callback);

  void prepareCSVDownload(SampleLike[] samples, Attribute[] attributes,
      AsyncCallback<String> callback);
}
