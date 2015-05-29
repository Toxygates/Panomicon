package otgviewer.client;

import java.util.ArrayList;
import java.util.List;

import otgviewer.client.components.Screen;
import t.common.shared.SampleClass;
import t.viewer.client.rpc.SparqlServiceAsync;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.SuggestOracle;

/**
 * This oracle looks up gene symbols in real time as the user types.
 * This is used to provide a list of autocomplete suggestions. Used in for example
 * the probe selection screen (manual selection) and the compound ranking screen.
 * @author johan
 *
 */
public class GeneOracle extends SuggestOracle {
	
	private SampleClass sampleClass;
	public void setFilter(SampleClass sc) {
		this.sampleClass = sc;
	}
	
	private static String lastRequest = "";
	
	private class GeneSuggestion implements Suggestion {
		private String geneId;
		public GeneSuggestion(String geneId) {
			this.geneId = geneId;
		}

		@Override
		public String getDisplayString() { return geneId; }

		@Override
		public String getReplacementString() { return geneId; }		
		
	}
	
	public GeneOracle(Screen screen) {
		sparqlService = screen.sparqlService();
	}
	
	private final SparqlServiceAsync sparqlService;
	
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
		sparqlService.geneSuggestions(sampleClass, request.getQuery(), new AsyncCallback<String[]>() {
			
			@Override
			public void onSuccess(String[] result) {
				List<Suggestion> ss = new ArrayList<Suggestion>();
				for (String sug: result) {
					ss.add(new GeneSuggestion(sug));
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
