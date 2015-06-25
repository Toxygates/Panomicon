package otgviewer.client.components;

import java.util.ArrayList;
import java.util.List;

import otgviewer.client.Utils;
import t.common.shared.Pair;
import t.viewer.client.rpc.SparqlServiceAsync;

import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.ValueBoxBase;
import com.google.gwt.user.client.ui.Widget;

public abstract class SearchTool extends DataListenerWidget {

	private final Screen screen;

	private static final String TEXTBOX_WATERMARK = "KEGG Pathway, GO term...";

	private final MultiSuggestBox msbKeyword;
	private final Button btnGo;
	private final CheckBox chkFilter;

	private HorizontalPanel tool;

	public SearchTool(Screen screen) {
		this.screen = screen;
		msbKeyword = new MultiSuggestBox(screen, TEXTBOX_WATERMARK);
		btnGo = new Button("Go");
		chkFilter = new CheckBox("filter by samples");

		makeTool();
	}

	public Widget tools() {
		return tool;
	}

	private void makeTool() {
		HorizontalPanel horizontalPanel = Utils.mkHorizontalPanel(true);
		horizontalPanel.setStylePrimaryName("colored");
		horizontalPanel.addStyleName("slightlySpaced");

		chkFilter.setValue(true);

		horizontalPanel.add(new Label("Search:"));
		horizontalPanel.add(msbKeyword);
		horizontalPanel.add(btnGo);
		horizontalPanel.add(chkFilter);

		tool = new HorizontalPanel();
		tool.add(horizontalPanel);
	}

	// Expected to be overridden by caller
	public abstract void keywordChanged(String keyword);
}

class MultiSuggestBox extends Composite
		implements
			SelectionHandler<Suggestion>,
			Focusable,
			FocusHandler,
			BlurHandler,
			KeyUpHandler {

	private final Screen screen;

	private final String WATERMARK;

	private SuggestBox field;

	public MultiSuggestBox(Screen screen, String watermark) {
		this.screen = screen;
		this.WATERMARK = watermark;
		createPanel();
	}

	private void createPanel() {
		HorizontalPanel panel = Utils.mkHorizontalPanel(true);

		ValueBoxBase<String> tb = new TextBox();
		enableWatermark(tb);

		SuggestOracle oracle = new KeywordSuggestOracle(screen);
		field = new SuggestBox(oracle, tb);

		tb.addFocusHandler(this);
		tb.addBlurHandler(this);
		tb.addKeyUpHandler(this);
		field.addSelectionHandler(this);

		panel.add(field);

		initWidget(panel);
	}

	@Override
	public void onKeyUp(KeyUpEvent event) {
		System.out.println("onKeyUp");
	}

	@Override
	public int getTabIndex() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setAccessKey(char key) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setFocus(boolean focused) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setTabIndex(int index) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSelection(SelectionEvent<Suggestion> event) {
		// TODO Auto-generated method stub

	}

	private void enableWatermark(ValueBoxBase<String> tb) {
		String t = tb.getText();
		if (t.length() == 0 || t.equalsIgnoreCase(WATERMARK)) {
			tb.setText(WATERMARK);
			tb.addStyleDependentName("watermark");
		}
	}

	public void disableWatermark(ValueBoxBase<String> tb) {
		String t = tb.getText();
		tb.removeStyleDependentName("watermark");
		if (t.equals(WATERMARK)) {
			tb.setText("");
		}
	}

	@Override
	public void onBlur(BlurEvent event) {
		enableWatermark(field.getValueBox());
	}

	@Override
	public void onFocus(FocusEvent event) {
		disableWatermark(field.getValueBox());
	}
}

class KeywordSuggestOracle extends SuggestOracle {

	private static String lastRequest = "";
	private final SparqlServiceAsync sparqlService;

	public KeywordSuggestOracle(Screen screen) {
		sparqlService = screen.sparqlService();
	}

	@Override
	public void requestSuggestions(final Request request,
			final Callback callback) {
		Timer t = new Timer() {
			@Override
			public void run() {
				if (lastRequest.equals(request.getQuery())
						&& !lastRequest.equals("")) {
					getSuggestions(request, callback);
				}
			}
		};
		lastRequest = request.getQuery();
		t.schedule(500);
	}

	private void getSuggestions(final Request request, final Callback callback) {
		sparqlService.keywordSuggestions(request.getQuery(),
				new AsyncCallback<Pair<String, String>[]>() {

					@Override
					public void onSuccess(Pair<String, String>[] result) {
						List<KeywordSuggestion> ss = new ArrayList<KeywordSuggestion>();
						for (Pair<String, String> sug : result) {
							ss.add(new KeywordSuggestion(sug.first(), sug
									.second(), request.getQuery()));
						}
						Response r = new Response(ss);
						callback.onSuggestionsReady(request, r);
					}

					@Override
					public void onFailure(Throwable caught) {

					}
				});
	}

	@Override
	public boolean isDisplayStringHTML() {
		return true;
	}

	private class KeywordSuggestion implements Suggestion {
		private String display;
		private String replacement;
		private String reference;

		public KeywordSuggestion(String keyword, String reference, String query) {
			this.display = keyword;
			this.replacement = keyword;
			this.reference = reference;

			int begin = keyword.toLowerCase().indexOf(query.toLowerCase());
			if (begin >= 0) {
				int end = begin + query.length();
				String match = keyword.substring(begin, end);
				this.display = keyword.replaceFirst(match, "<b>" + match
						+ "</b>");
			} else {
				this.display = keyword;
			}
			
			this.display = getFullDisplayString(this.display, this.reference);
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
			return replacement;
		}

		public String getReference() {
			return reference;
		}
	}

}
