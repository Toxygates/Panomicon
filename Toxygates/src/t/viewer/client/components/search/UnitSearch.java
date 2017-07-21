package t.viewer.client.components.search;

import com.google.gwt.user.client.rpc.AsyncCallback;

import t.common.shared.sample.Unit;
import t.model.SampleClass;
import t.viewer.client.Analytics;
import t.viewer.client.rpc.SampleServiceAsync;

public class UnitSearch extends Search<Unit> {
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
}
