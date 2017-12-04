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


public class SampleSearch extends Search<Sample, Pair<Sample, Pair<Unit, Unit>>> {
  private HashMap<String, Sample> sampleIdHashMap;
  private HashMap<String, Pair<Unit, Unit>> unitPairsMap;

  public SampleSearch(Delegate delegate, ResultTable<Sample> helper,
                      AttributeSet attributes,
      SampleServiceAsync sampleService) {
    super(delegate, helper, attributes, sampleService);
  }

  @Override
  protected void extractSearchResult(RequestResult<Pair<Sample, Pair<Unit, Unit>>> result) {
    List<Sample> samples = new ArrayList<Sample>();
    unitPairsMap = new HashMap<String, Pair<Unit, Unit>>();
    for (Pair<Sample, Pair<Unit, Unit>> pair: result.items()) {
      samples.add(pair.first());
      unitPairsMap.put(pair.first().get(SampleId), pair.second());
    }
    searchResult = samples.toArray(new Sample[0]);
  }

  @Override
  protected void asyncSearch(SampleClass sampleClass,
      AsyncCallback<RequestResult<Pair<Sample, Pair<Unit, Unit>>>> callback) {
    sampleService.sampleSearch(sampleClass, condition, MAX_RESULTS, callback);
  }

  @Override
  protected void trackAnalytics() {
    Analytics.trackEvent(Analytics.CATEGORY_ANALYSIS, Analytics.ACTION_PERFORM_SAMPLE_SEARCH);
  }

  @Override
  protected void searchComplete(RequestResult<Pair<Sample, Pair<Unit, Unit>>> result) {
    super.searchComplete(result);
    sampleIdHashMap = null;
  }

  @Override
  protected Sample[] relevantSamples() {
    return searchResult;
  }

  private Map<String, Sample> sampleIdMap() {
    if (sampleIdHashMap == null) {
      sampleIdHashMap = new HashMap<String, Sample>();
      for (Sample sample : searchResult) {
        sampleIdHashMap.put(sample.id(), sample);
      }
    }
    return sampleIdHashMap;
  }

  @Override
  protected void addParameter(Attribute attribute, Annotation[] annotations) {
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
  }

  @Override
  public Unit[] sampleGroupFromEntities(Collection<Sample> samples) {
    Set<Unit> allUnits = new HashSet<Unit>();
    for (Sample sample : samples) {
      Pair<Unit, Unit> units = unitPairsMap.get(sample.get(SampleId));
      allUnits.add(units.first());
      allUnits.add(units.second());
    }
    return allUnits.toArray(new Unit[0]);
  }
}
