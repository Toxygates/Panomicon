/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
 * (NIBIOHN), Japan.
 *
 * This file is part of Toxygates.
 * 
 * Toxygates is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * Toxygates is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Toxygates. If not,
 * see <http://www.gnu.org/licenses/>.
 */

package otg.viewer.client.components;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.SuggestOracle;

import t.common.shared.Pair;
import t.model.SampleClass;
import t.viewer.client.rpc.ProbeServiceAsync;

/**
 * This oracle looks up gene symbols in real time as the user types. This is used to provide a list
 * of autocomplete suggestions. Used in for example the probe selection screen (manual selection)
 * and the compound ranking screen.
 */
public class GeneOracle extends SuggestOracle {

  private SampleClass sampleClass;
  private Screen screen;

  public void setFilter(SampleClass sc) {
    this.sampleClass = sc;
  }

  private static String lastRequest = "";

  private class GeneSuggestion implements Suggestion {
    private String symbol;
    private String probeId;

    public GeneSuggestion(String symbol, String probeId) {
      this.symbol = symbol;
      this.probeId = probeId;
    }

    @Override
    public String getDisplayString() {
      return symbol + " (" + probeId + ")";
    }

    @Override
    public String getReplacementString() {
      return symbol;
    }

  }

  public GeneOracle(Screen screen) {
    probeService = screen.manager().probeService();
    this.screen = screen;
  }

  private final ProbeServiceAsync probeService;

  @Override
  public void requestSuggestions(final Request request, final Callback callback) {
    Timer t = new Timer() {
      @Override
      public void run() {
        if (lastRequest.equals(request.getQuery()) && !lastRequest.equals("")) {
          getSuggestions(request, callback);
        }
      }
    };
    lastRequest = request.getQuery();
    t.schedule(500);
  }

  private void getSuggestions(final Request request, final Callback callback) {
    probeService.geneSuggestions(sampleClass, request.getQuery(),
        new AsyncCallback<Pair<String, String>[]>() {
          @Override
          public void onSuccess(Pair<String, String>[] result) {
            List<Suggestion> ss = new ArrayList<Suggestion>();
            for (Pair<String, String> sug : result) {
              ss.add(new GeneSuggestion(sug.first(), sug.second()));
            }
            Response r = new Response(ss);
            callback.onSuggestionsReady(request, r);
          }

          @Override
          public void onFailure(Throwable caught) {
            screen.getLogger()
                .warning("Unable to get gene suggestions for request: " + request.getQuery());
          }
    });
  }
}
