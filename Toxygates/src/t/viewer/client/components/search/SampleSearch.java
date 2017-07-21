package t.viewer.client.components.search;

import com.google.gwt.user.client.rpc.AsyncCallback;

import t.common.shared.sample.Sample;
import t.model.SampleClass;
import t.viewer.client.Analytics;
import t.viewer.client.rpc.SampleServiceAsync;

public class SampleSearch extends Search<Sample> {
  public SampleSearch(Delegate delegate, ResultTable<Sample> helper,
      SampleServiceAsync sampleService) {
    super(delegate, helper, sampleService);
  }

  @Override
  protected void asyncSearch(SampleClass sampleClass, AsyncCallback<Sample[]> callback) {
    sampleService.sampleSearch(sampleClass, condition, callback);
  }

  @Override
  protected void trackAnalytics() {
    Analytics.trackEvent(Analytics.CATEGORY_ANALYSIS, Analytics.ACTION_PERFORM_SAMPLE_SEARCH);
  }
}
