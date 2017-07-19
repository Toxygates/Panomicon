package t.viewer.client.components.search;

import javax.annotation.Nullable;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

import t.common.shared.sample.Sample;
import t.common.shared.sample.Unit;
import t.common.shared.sample.search.MatchCondition;
import t.model.SampleClass;
import t.viewer.client.Analytics;
import t.viewer.client.rpc.SampleServiceAsync;

interface SearchDelegate {
  void searchStarted(Search<?> search);
  void searchEnded(Search<?> search, int numResults);
}

/**
 * Makes asynchronous sample/unit search requests to the SampleService and reports back on the
 * results
 */
abstract class Search<T> {
  protected SearchDelegate delegate;
  protected ResultTable<T> helper;
  protected SampleServiceAsync sampleService;
  protected SampleClass sampleClass;

  T[] searchResult;
  MatchCondition condition;

  abstract void asyncSearch(AsyncCallback<T[]> callback);
  abstract void trackAnalytics();

  public Search(SearchDelegate delegate, ResultTable<T> helper, SampleServiceAsync sampleService,
      SampleClass sampleClass) {
    this.delegate = delegate;
    this.helper = helper;
    this.sampleService = sampleService;
    this.sampleClass = sampleClass;
  }

  public void attemptSearch(final @Nullable MatchCondition condition) {
    if (condition == null) {
      Window.alert("Please define the search condition.");
      return;
    }

    this.condition = condition;
    delegate.searchStarted(Search.this);
    trackAnalytics();

    asyncSearch(new AsyncCallback<T[]>() {
      @Override
      public void onSuccess(T[] result) {
        searchResult = result;
        delegate.searchEnded(Search.this, result.length);
        helper.setupTable(result, condition);
      }

      @Override
      public void onFailure(Throwable caught) {
        Window.alert("Failure: " + caught);
      }
    });
  }
}

class SampleSearch extends Search<Sample> {
  public SampleSearch(SearchDelegate delegate, ResultTable<Sample> helper,
      SampleServiceAsync sampleService, SampleClass sampleClass) {
    super(delegate, helper, sampleService, sampleClass);
  }

  @Override
  protected void asyncSearch(AsyncCallback<Sample[]> callback) {
    sampleService.sampleSearch(sampleClass, condition, callback);
  }

  @Override
  protected void trackAnalytics() {
    Analytics.trackEvent(Analytics.CATEGORY_ANALYSIS, Analytics.ACTION_PERFORM_SAMPLE_SEARCH);
  }
}

class UnitSearch extends Search<Unit> {
  public UnitSearch(SearchDelegate delegate, ResultTable<Unit> helper,
      SampleServiceAsync sampleService, SampleClass sampleClass) {
    super(delegate, helper, sampleService, sampleClass);
  }

  @Override
  protected void asyncSearch(AsyncCallback<Unit[]> callback) {
    sampleService.unitSearch(sampleClass, condition, callback);
  }

  @Override
  protected void trackAnalytics() {
    Analytics.trackEvent(Analytics.CATEGORY_ANALYSIS, Analytics.ACTION_PERFORM_UNIT_SEARCH);
  }
}
