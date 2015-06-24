package otgviewer.client.components;

import otgviewer.client.Utils;

import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public abstract class SearchTool extends DataListenerWidget {

	private final Screen screen;

	private final String SEARCHBOX_DEFAULT_VALUE = "KEGG Pathwat, GO term...";
	private boolean textIsEmpty = true;
	
	private final TextBox txbKeyword;
	private final Button btnGo;
	private final CheckBox chkFilter;

	private HorizontalPanel tool;

	public SearchTool(Screen screen) {
		this.screen = screen;
		txbKeyword = new TextBox();
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

		horizontalPanel.add(new Label("Search:"));
		
		txbKeyword.addFocusHandler(new FocusHandler() {
			@Override
			public void onFocus(FocusEvent event) {
				System.out.println("On focus");
			}
		});
		txbKeyword.addBlurHandler(new BlurHandler() {
			@Override
			public void onBlur(BlurEvent event) {
				if (txbKeyword.getText().length() == 0) {
					// TODO 
				}
			}
		});
		horizontalPanel.add(txbKeyword);
		
		horizontalPanel.add(btnGo);
		
		chkFilter.setValue(true);
		horizontalPanel.add(chkFilter);

		tool = new HorizontalPanel();
		tool.add(horizontalPanel);
	}

	// Expected to be overridden by caller
	public abstract void keywordChanged(String keyword);
}
