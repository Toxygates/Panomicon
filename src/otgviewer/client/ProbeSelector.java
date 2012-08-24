package otgviewer.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * An interface component that helps users to select probes
 * using some kind of intermediate concept (pathway, GO term etc)
 * @author johan
 *
 */
abstract public class ProbeSelector extends DataListenerWidget {

	TextBox searchBox;
	ListBox itemList;
	ListSelectionHandler<String> itemHandler;
	
	private OwlimServiceAsync owlimService = (OwlimServiceAsync) GWT
			.create(OwlimService.class);

	
	public ProbeSelector(String label) {
		VerticalPanel vp = new VerticalPanel();
		initWidget(vp);
		vp.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		
		Label lblPathwaySearch = new Label(label);		
		vp.add(lblPathwaySearch);
		lblPathwaySearch.setWidth("100%");

		searchBox = new TextBox();
		vp.add(searchBox);
		searchBox.setWidth("90%");
		searchBox.addKeyPressHandler(new KeyPressHandler() {
			public void onKeyPress(KeyPressEvent event) {
				if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER) {
					itemHandler.clearForLoad();
					getMatches(searchBox.getText());					
				}
			}
		});
		

		itemList = new ListBox();
		vp.add(itemList);
		itemList.setSize("100%", "300px");
		itemList.setVisibleItemCount(5);

		itemHandler = new ListSelectionHandler<String>("pathways",
				itemList, false) {
			protected void getUpdates(String item) {
				getProbes(item);
						
			}
		};
	}
	
	public AsyncCallback<String[]> retrieveMatchesCallback() {
		return itemHandler.retrieveCallback();
	}
	
	abstract protected void getMatches(String key);
	
	public AsyncCallback<String[]> retrieveProbesCallback() {
		return new AsyncCallback<String[]>() {
			public void onFailure(Throwable caught) {
				Window.alert("Unable to get probes.");
				itemHandler.clear();
			}

			public void onSuccess(String[] probes) {
				probesChanged(probes);								
			}
		};
	}	
	
	abstract protected void getProbes(String item);
	
}
