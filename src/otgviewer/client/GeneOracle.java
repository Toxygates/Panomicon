package otgviewer.client;

import java.util.ArrayList;
import java.util.List;

import otgviewer.shared.DataFilter;

import bioweb.shared.Pair;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.SuggestOracle;

/**
 * This oracle looks up gene descriptions and names in real time as the user types.
 * This is used to provide a list of autocomplete suggestions. Used in for example
 * the probe selection screen (manual selection) and the compound ranking screen.
 * @author johan
 *
 */
public class GeneOracle extends SuggestOracle {
	
	private DataFilter filter;
	public void setFilter(DataFilter filter) {
		this.filter = filter;
	}
	
	private static String lastRequest = "";
	
	private class GeneSuggestion implements Suggestion {
		private String geneId, fullName;
		public GeneSuggestion(String fullName, String geneId) {
			this.geneId = geneId;
			this.fullName = fullName;
		}

		@Override
		public String getDisplayString() { return fullName; }

		@Override
		public String getReplacementString() { return geneId; }		
		
	}
	
	private SparqlServiceAsync owlimService = (SparqlServiceAsync) GWT
			.create(SparqlService.class);
	
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
		owlimService.geneSuggestions(filter, request.getQuery(), new AsyncCallback<Pair<String,String>[]>() {
			
			@Override
			public void onSuccess(Pair<String, String>[] result) {
				List<Suggestion> ss = new ArrayList<Suggestion>();
				for (Pair<String, String> sug: result) {
					ss.add(new GeneSuggestion(sug.second(), sug.first()));
				}
				Response r = new Response(ss);
				callback.onSuggestionsReady(request, r);
			}
			
			
			@Override
			public void onFailure(Throwable caught) {
				// TODO Auto-generated method stub
				
			}
		});
	}

}
