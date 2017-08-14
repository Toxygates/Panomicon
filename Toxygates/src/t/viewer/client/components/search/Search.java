package t.viewer.client.components.search;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import t.common.shared.RequestResult;
import t.common.shared.sample.Annotation;
import t.common.shared.sample.HasSamples;
import t.common.shared.sample.Sample;
import t.common.shared.sample.search.MatchCondition;
import t.model.SampleClass;
import t.model.sample.AttributeSet;
import t.viewer.client.rpc.SampleServiceAsync;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Makes asynchronous sample/unit search requests to the SampleService and reports back on the
 * results
 * 
 * @tparam EntityType the type of objects that are searched for
 * @tparam ContainerType the type of object (usually some kind of collection of EntityType)
 *         representing a search result
 */
public abstract class Search<Entity, Container> {
  public interface Delegate {
    void searchStarted(Search<?, ?> search);
    void searchEnded(Search<?, ?> search, String resultCountText);
  }

  final static int MAX_RESULTS = 1000;

  protected Delegate delegate;
  protected ResultTable<Entity> helper;
  protected SampleServiceAsync sampleService;
  protected AttributeSet attributes;
  
  Entity[] searchResult;
  MatchCondition condition;
  private Set<String> fetchedParameters;

  public Search(Delegate delegate, ResultTable<Entity> helper,
                AttributeSet attributes,
      SampleServiceAsync sampleService) {
    this.delegate = delegate;
    this.helper = helper;
    this.sampleService = sampleService;
    this.attributes = attributes;
  }

  public ResultTable<Entity> helper() {
    return helper;
  }

  public Entity[] searchResult() {
    return searchResult;
  }

  /**
   * Extract an array of EntityType and save it to searchResult
   * 
   * @param result the object returned from a search on the backend
   */
  abstract void extractSearchResult(RequestResult<Container> result);

  protected void searchComplete(RequestResult<Container> result) {
    extractSearchResult(result);
    fetchedParameters = new HashSet<String>();
    if (result.totalCount() <= MAX_RESULTS) {
      delegate.searchEnded(Search.this, "Found " + result.totalCount() + " results");
    } else {
      delegate.searchEnded(Search.this,
          "Displaying " + MAX_RESULTS + " of " + result.totalCount() + " results");
      Window.alert(
          "Too many search results; only the first " + MAX_RESULTS + " results will be displayed.");
    }
    helper.setupTable(searchResult, condition);
  }

  abstract void asyncSearch(SampleClass sampleClass,
      AsyncCallback<RequestResult<Container>> callback);
  abstract void trackAnalytics();

  public void attemptSearch(SampleClass sampleClass, final @Nullable MatchCondition condition) {
    if (condition == null) {
      Window.alert("Please define the search condition.");
      return;
    }

    this.condition = condition;
    delegate.searchStarted(Search.this);
    trackAnalytics();

    asyncSearch(sampleClass, new AsyncCallback<RequestResult<Container>>() {
      @Override
      public void onSuccess(RequestResult<Container> result) {
        searchComplete(result);
      }

      @Override
      public void onFailure(Throwable caught) {
        Window.alert("Failure: " + caught);
      }
    });
  }

  public boolean hasParameter(String parameterId) {
    return fetchedParameters.contains(parameterId);
  }

  protected void getAnnotationsAsync(String parameterId, AsyncCallback<Annotation[]> callback) {
    // TODO: only get specified parameter
    HasSamples<Sample> hasSamples = new SampleContainer(relevantSamples());
    sampleService.annotations(hasSamples, false, callback);
  }

  abstract Sample[] relevantSamples();
  abstract void addParameter(String parameterId, Annotation[] annotations);

  public void fetchParameter(final String parameterId) {
    getAnnotationsAsync(parameterId, new AsyncCallback<Annotation[]>() {
      @Override
      public void onSuccess(Annotation[] result) {
        addParameter(parameterId, result);
        fetchedParameters.add(parameterId);
        helper.gotDataForKey(parameterId);
        helper.cellTable().redraw();
      }

      @Override
      public void onFailure(Throwable caught) {
        Window.alert("Failure: " + caught);
      }
    });
  }
}

