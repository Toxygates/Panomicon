/*
 * Copyright (c) 2012-2017 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
 * (NIBIOHN), Japan.
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
package otgviewer.client;

import java.util.*;

import otgviewer.client.components.*;
import t.common.shared.*;
import t.viewer.client.rpc.ProbeServiceAsync;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.SuggestOracle;

public class TermSuggestOracle extends SuggestOracle implements
    HasExactMatchHandler<Term> {

  private final ProbeServiceAsync probeService;

  private String lastRequest = "";

  private List<ExactMatchHandler<Term>> handlers =
      new ArrayList<ExactMatchHandler<Term>>();

  public TermSuggestOracle(Screen screen) {
    probeService = screen.manager().probeService();
  }

  @Override
  public void addExactMatchHandler(ExactMatchHandler<Term> handler) {
    handlers.add(handler);
  }

  @Override
  public void requestSuggestions(final Request request, final Callback callback) {
    Timer t = new Timer() {
      @Override
      public void run() {
        // avoid executing if query has changed
        if (lastRequest.equals(request.getQuery()) && !lastRequest.equals("")) {
          getSuggestions(request, callback);
        }
      }
    };
    lastRequest = request.getQuery();
    t.schedule(500);
  }

  public void getSuggestions(final Request request, final Callback callback) {
    probeService.keywordSuggestions(request.getQuery(), 5,
        new AsyncCallback<Pair<String, AType>[]>() {
          @Override
          public void onSuccess(Pair<String, AType>[] result) {
            List<Suggestion> suggestions =
                generateSuggestions(Arrays.asList(result), request.getQuery());

            checkExactMatches(suggestions, request.getQuery());
            callback.onSuggestionsReady(request, new Response(suggestions));
          }

          @Override
          public void onFailure(Throwable caught) {}
        });
  }

  private List<Suggestion> generateSuggestions(
      List<Pair<String, AType>> result, String query) {
    List<Suggestion> r = new ArrayList<Suggestion>();
    for (Pair<String, AType> sug : result) {
      r.add(new TermSuggestion(sug.first(), sug.second(), query));
    }
    return r;
  }

  private void checkExactMatches(List<Suggestion> suggestions, String query) {
    List<Term> exactMatches = new ArrayList<>();
    for (Suggestion s : suggestions) {
      TermSuggestion ks = (TermSuggestion) s;
      if (ks.getText().equalsIgnoreCase(query)) {
        exactMatches.add(new Term(ks.getText(), ks.getAssociation()));
      }
    }

    if (exactMatches.size() > 1) {
      onExactMatchFound(exactMatches);
    }
  }

  private void onExactMatchFound(List<Term> exactMatches) {
    for (ExactMatchHandler<Term> h : handlers) {
      h.onExactMatchFound(exactMatches);
    }
  }

  @Override
  public boolean isDisplayStringHTML() {
    return true;
  }

  public class TermSuggestion implements Suggestion {
    private Term term;
    private String display;

    public TermSuggestion(String term, AType association, String query) {
      this(new Term(term, association), query);
    }

    public TermSuggestion(Term term, String query) {
      this.term = term;

      String plain = term.getTermString();
      int begin = plain.toLowerCase().indexOf(query.toLowerCase());
      if (begin >= 0) {
        int end = begin + query.length();
        String match = plain.substring(begin, end);
        display = plain.replaceFirst(match, "<b>" + match + "</b>");
      } else {
        display = plain;
      }

      display = getFullDisplayString(display, term.getAssociation().title());
    }

    private String getFullDisplayString(String display, String reference) {
      StringBuffer sb = new StringBuffer();
      sb.append("<div class=\"suggest-item\">");
      sb.append("<div class=\"suggest-keyword\">");
      sb.append(display);
      sb.append("</div>");
      sb.append("<div class=\"suggest-reference\">");
      sb.append(reference);
      sb.append("</div>");
      sb.append("</div>");

      return sb.toString();
    }

    @Override
    public String getDisplayString() {
      return display;
    }

    @Override
    public String getReplacementString() {
      return term.getTermString();
    }

    public Term getTerm() {
      return term;
    }

    public String getText() {
      return term.getTermString();
    }

    public AType getAssociation() {
      return term.getAssociation();
    }
  }

}
