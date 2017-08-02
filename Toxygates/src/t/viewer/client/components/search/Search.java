package t.viewer.client.components.search;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

import t.common.shared.sample.Annotation;
import t.common.shared.sample.HasSamples;
import t.common.shared.sample.Sample;
import t.common.shared.sample.search.MatchCondition;
import t.model.SampleClass;
import t.viewer.client.rpc.SampleServiceAsync;

/**
 * Makes asynchronous sample/unit search requests to the SampleService and reports back on the
 * results
 * 
 * @tparam EntityType the type of objects that are searched for
 * @tparam ContainerType the type of object (usually some kind of collection of EntityType)
 *         representing a search result
 */
public abstract class Search<EntityType, ContainerType> {
  public interface Delegate {
    void searchStarted(Search<?, ?> search);
    void searchEnded(Search<?, ?> search, int numResults);
  }

  protected Delegate delegate;
  protected ResultTable<EntityType> helper;
  protected SampleServiceAsync sampleService;

  EntityType[] searchResult;
  MatchCondition condition;
  private Set<String> fetchedParameters;

  public Search(Delegate delegate, ResultTable<EntityType> helper,
      SampleServiceAsync sampleService) {
    this.delegate = delegate;
    this.helper = helper;
    this.sampleService = sampleService;
  }

  public ResultTable<EntityType> helper() {
    return helper;
  }

  public EntityType[] searchResult() {
    return searchResult;
  }

  /**
   * Extract an array of EntityType and save it to searchResult
   * 
   * @param result the object returned from a search on the backend
   */
  abstract void extractSearchResult(ContainerType result);

  protected void searchComplete(ContainerType result) {
    extractSearchResult(result);
    fetchedParameters = new HashSet<String>();
    delegate.searchEnded(Search.this, searchResult.length);
    helper.setupTable(searchResult, condition);
  }

  abstract void asyncSearch(SampleClass sampleClass, AsyncCallback<ContainerType> callback);
  abstract void trackAnalytics();

  public void attemptSearch(SampleClass sampleClass, final @Nullable MatchCondition condition) {
    if (condition == null) {
      Window.alert("Please define the search condition.");
      return;
    }

    this.condition = condition;
    delegate.searchStarted(Search.this);
    trackAnalytics();

    asyncSearch(sampleClass, new AsyncCallback<ContainerType>() {
      @Override
      public void onSuccess(ContainerType result) {
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

