package t.viewer.client.components.search;

import static t.model.sample.CoreParameter.SampleId;

import java.util.*;

import t.common.shared.Pair;
import t.common.shared.RequestResult;
import t.common.shared.sample.*;
import t.model.SampleClass;
import t.model.sample.Attribute;
import t.model.sample.AttributeSet;
import t.viewer.client.Analytics;
import t.viewer.client.rpc.SampleServiceAsync;

import com.google.gwt.user.client.rpc.AsyncCallback;

public class UnitSearch extends Search<Unit, Pair<Unit, Unit>> {
  private Sample[] samplesInResult;
  private HashMap<String, Sample> sampleIdHashMap;
  private HashMap<String, Unit> controlUnitsMap;

  public UnitSearch(Delegate delegate, ResultTable<Unit> helper, 
                    AttributeSet attributes, SampleServiceAsync sampleService) {
    super(delegate, helper, attributes, sampleService);
  }

  @Override
  protected void extractSearchResult(RequestResult<Pair<Unit, Unit>> result) {
    List<Unit> units = new ArrayList<Unit>();
    controlUnitsMap = new HashMap<String, Unit>();
    for (Pair<Unit, Unit> pair : result.items()) {
      units.add(pair.first());
      controlUnitsMap.put(pair.first().get(SampleId), pair.second());
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
  protected void addParameter(Attribute attribute, Annotation[] annotations) {
    // first load parameter info into samples
    for (Annotation annotation : annotations) {
      Sample sample = sampleIdMap().get(annotation.id());
      if (sample != null) {
        for (BioParamValue value : annotation.getAnnotations()) {
          if (value.id() == attribute.id()) {
            sample.sampleClass().put(attribute, value.displayValue());
            break;
          }
        }
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
      allUnits.add(controlUnitsMap.get(unit.get(SampleId)));
    }
    return allUnits.toArray(new Unit[0]);
  }
}
