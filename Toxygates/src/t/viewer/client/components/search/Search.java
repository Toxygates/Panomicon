package t.viewer.client.components.search;

import javax.annotation.Nullable;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

import t.common.shared.sample.search.MatchCondition;
import t.model.SampleClass;
import t.viewer.client.rpc.SampleServiceAsync;

/**
 * Makes asynchronous sample/unit search requests to the SampleService and reports back on the
 * results
 */
public abstract class Search<T> {
  public interface Delegate {
    void searchStarted(Search<?> search);
    void searchEnded(Search<?> search, int numResults);
  }

  protected Delegate delegate;
  protected ResultTable<T> helper;
  protected SampleServiceAsync sampleService;

  T[] searchResult;
  MatchCondition condition;

  abstract void asyncSearch(SampleClass sampleClass, AsyncCallback<T[]> callback);
  abstract void trackAnalytics();

  public Search(Delegate delegate, ResultTable<T> helper, SampleServiceAsync sampleService) {
    this.delegate = delegate;
    this.helper = helper;
    this.sampleService = sampleService;
  }

  public ResultTable<T> helper() {
    return helper;
  }

  public T[] searchResult() {
    return searchResult;
  }

  public void attemptSearch(SampleClass sampleClass, final @Nullable MatchCondition condition) {
    if (condition == null) {
      Window.alert("Please define the search condition.");
      return;
    }

    this.condition = condition;
    delegate.searchStarted(Search.this);
    trackAnalytics();

    asyncSearch(sampleClass, new AsyncCallback<T[]>() {
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

