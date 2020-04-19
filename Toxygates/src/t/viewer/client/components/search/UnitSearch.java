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

package t.viewer.client.components.search;

import java.util.*;

import com.google.gwt.user.client.rpc.AsyncCallback;

import t.common.shared.Pair;
import t.common.shared.RequestResult;
import t.common.shared.sample.*;
import t.model.SampleClass;
import t.model.sample.*;
import t.viewer.client.Analytics;
import t.viewer.client.rpc.SampleServiceAsync;

public class UnitSearch extends Search<Unit, Pair<Unit, Unit>> {
  private Sample[] samplesInResult;
  private HashMap<String, Sample> sampleIdHashMap;
  private HashMap<String, Unit> controlUnitsMap;

  public UnitSearch(Delegate delegate, ResultTable<Unit> helper, 
                    SampleServiceAsync sampleService) {
    super(delegate, helper, sampleService);
  }

  @Override
  protected void extractSearchResult(RequestResult<Pair<Unit, Unit>> result) {
    List<Unit> units = new ArrayList<Unit>();
    controlUnitsMap = new HashMap<String, Unit>();
    for (Pair<Unit, Unit> pair : result.items()) {
      units.add(pair.first());
      controlUnitsMap.put(pair.first().get(CoreParameter.SampleId), pair.second());
    }
    searchResult = units.toArray(new Unit[0]);
  }

  @Override
  protected void asyncSearch(SampleClass sampleClass,
      AsyncCallback<RequestResult<Pair<Unit, Unit>>> callback) {
    sampleService.unitSearch(sampleClass, condition, MAX_RESULTS, callback);
  }

  @Override
  protected void trackAnalytics() {
    Analytics.trackEvent(Analytics.CATEGORY_ANALYSIS, Analytics.ACTION_PERFORM_UNIT_SEARCH);
  }

  @Override
  protected void searchComplete(RequestResult<Pair<Unit, Unit>> result) {
    super.searchComplete(result);
    samplesInResult = null;
  }

  @Override
  protected Sample[] relevantSamples() {
    if (samplesInResult == null) {
      ArrayList<Sample> samples = new ArrayList<Sample>();
      sampleIdHashMap = new HashMap<String, Sample>();
      for (Unit unit : searchResult) {
        for (Sample sample : unit.getSamples()) {
          samples.add(sample);
          sampleIdHashMap.put(sample.id(), sample);
        }
      }
      samplesInResult = samples.toArray(new Sample[0]);
    }
    return samplesInResult;
  }

  private Map<String, Sample> sampleIdMap() {
    relevantSamples(); // just to make sure the HashMap has been created
    return sampleIdHashMap;
  }

  @Override
  protected void addParameter(Attribute attribute, Map<String, HashMap<Attribute, String>> parameterValues) {
    // first load parameter info into samples
    for (Map.Entry<String, HashMap<Attribute, String>> item : parameterValues.entrySet()) {
      Sample sample = sampleIdMap().get(item.getKey());
      if (sample != null) {
        String value = item.getValue().get(attribute);
        sample.sampleClass().put(attribute, value);
      }
    }
    
    // then compute parameter value for each unit
    for (Unit unit : searchResult) {
      if (attribute.isNumerical()) {
        unit.averageAttribute(attribute);
      } else {
        unit.concatenateAttribute(attribute);
      }
    }
  }

  @Override
  public Unit[] sampleGroupFromEntities(Collection<Unit> units) {
    List<Unit> allUnits = new ArrayList<Unit>();
    for (Unit unit : units) {
      allUnits.add(unit);
      allUnits.add(controlUnitsMap.get(unit.get(CoreParameter.SampleId)));
    }
    return allUnits.toArray(new Unit[0]);
  }
}
