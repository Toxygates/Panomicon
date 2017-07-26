package t.viewer.client.components.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;

import t.common.shared.sample.Annotation;
import t.common.shared.sample.BioParamValue;
import t.common.shared.sample.NumericalBioParamValue;
import t.common.shared.sample.Sample;
import t.common.shared.sample.Unit;
import t.model.SampleClass;
import t.viewer.client.Analytics;
import t.viewer.client.rpc.SampleServiceAsync;

public class UnitSearch extends Search<Unit> {
  private Sample[] samplesInResult;
  private HashMap<String, Sample> sampleIdHashMap;

  public UnitSearch(Delegate delegate, ResultTable<Unit> helper, SampleServiceAsync sampleService) {
    super(delegate, helper, sampleService);
  }

  @Override
  protected void asyncSearch(SampleClass sampleClass, AsyncCallback<Unit[]> callback) {
    sampleService.unitSearch(sampleClass, condition, callback);
  }

  @Override
  protected void trackAnalytics() {
    Analytics.trackEvent(Analytics.CATEGORY_ANALYSIS, Analytics.ACTION_PERFORM_UNIT_SEARCH);
  }

  @Override
  protected void searchComplete(Unit[] result) {
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
  protected void addParameter(String parameterId, Annotation[] annotations) {
    // first load parameter info into samples
    for (Annotation annotation : annotations) {
      Sample sample = sampleIdMap().get(annotation.id());
      if (sample != null) {
        for (BioParamValue value : annotation.getAnnotations()) {
          if (value.id() == parameterId) {
            sample.sampleClass().put(parameterId, value.displayValue());
            break;
          }
        }
      }
    }

    // check if parameter is numeric; someday this step should become trivial
    boolean parameterIsNumeric = false;
    for (BioParamValue value : annotations[0].getAnnotations()) {
      if (value.id() == parameterId) {
        parameterIsNumeric = (value instanceof NumericalBioParamValue);
      }
    }

    // then compute parameter value for each unit
    for (Unit unit : searchResult) {
      if (parameterIsNumeric) {
        unit.averageAttribute(parameterId);
      } else {
        unit.concatenateAttribute(parameterId);
      }
    }
  }
}
