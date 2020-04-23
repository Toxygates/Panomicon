/*
 * Copyright (c) 2012-2019 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition (NIBIOHN), Japan.
 *
 * This file is part of Toxygates.
 *
 * Toxygates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Toxygates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Toxygates. If not, see <http://www.gnu.org/licenses/>.
 */

package t.viewer.client.components.search;

import java.util.*;

import javax.annotation.Nullable;

import t.common.shared.RequestResult;
import t.common.shared.sample.*;
import t.common.shared.sample.search.MatchCondition;
import t.model.SampleClass;
import t.model.sample.Attribute;
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

  Entity[] searchResult;
  MatchCondition condition;
  private Set<Attribute> fetchedAttributes;

  public Search(Delegate delegate, ResultTable<Entity> helper,
      SampleServiceAsync sampleService) {
    this.delegate = delegate;
    this.helper = helper;
    this.sampleService = sampleService;
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
    fetchedAttributes = new HashSet<Attribute>();
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

  public boolean hasParameter(Attribute attribute) {
    return fetchedAttributes.contains(attribute);
  }

  protected void getAnnotationsAsync(Attribute attribute,
                                     AsyncCallback<Sample[]> callback) {
    sampleService.parameterValuesForSamples(relevantSamples(), new Attribute[] {attribute}, callback);
  }

  abstract Sample[] relevantSamples();

  abstract void addParameter(Attribute attribute, Sample[] fetchedSamples);

  public void fetchParameter(final Attribute attribute) {
    getAnnotationsAsync(attribute, new AsyncCallback<Sample[]>() {
      @Override
      public void onSuccess(Sample[] result) {
        addParameter(attribute, result);
        fetchedAttributes.add(attribute);
        helper.gotDataForAttribute(attribute);
        helper.cellTable().redraw();
      }

      @Override
      public void onFailure(Throwable caught) {
        Window.alert("Failure: " + caught);
      }
    });
  }

  public Unit[] sampleGroupFromSelected() {
    return sampleGroupFromEntities(helper.selectionTable().getSelection());
  }

  abstract Unit[] sampleGroupFromEntities(Collection<Entity> entities);
}

