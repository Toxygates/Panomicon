package t.viewer.client.components.search;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;

import t.common.shared.sample.Annotation;
import t.common.shared.sample.BioParamValue;
import t.common.shared.sample.Sample;
import t.model.SampleClass;
import t.viewer.client.Analytics;
import t.viewer.client.rpc.SampleServiceAsync;

public class SampleSearch extends Search<Sample, Sample[]> {
  private HashMap<String, Sample> sampleIdHashMap;

  public SampleSearch(Delegate delegate, ResultTable<Sample> helper,
      SampleServiceAsync sampleService) {
    super(delegate, helper, sampleService);
  }

  @Override
  protected void extractSearchResult(Sample[] result) {
    searchResult = result;
  }

  @Override
  protected void asyncSearch(SampleClass sampleClass, AsyncCallback<Sample[]> callback) {
    sampleService.sampleSearch(sampleClass, condition, callback);
  }

  @Override
  protected void trackAnalytics() {
    Analytics.trackEvent(Analytics.CATEGORY_ANALYSIS, Analytics.ACTION_PERFORM_SAMPLE_SEARCH);
  }

  @Override
  protected void searchComplete(Sample[] result) {
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
  protected void addParameter(String parameterId, Annotation[] annotations) {
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
  }
}
