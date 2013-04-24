package otgviewer.client;

import otgviewer.client.components.DataListenerWidget;
import otgviewer.client.components.ListSelectionHandler;
import otgviewer.client.components.PendingAsyncCallback;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * An interface component that helps users to select probes
 * using some kind of higher level concept (pathway, GO term etc)
 * 
 * Probe selection is a two-step process.
 * First the user enters a partial name. A RPC call will then search for items
 * matching that name (for example pathways). The hits will be displayed.
 * Next, when the user selects one such object, the corresponding probes will be obtained.
 * 
 * @author johan
 *
 */
abstract public class ProbeSelector extends DataListenerWidget {

	TextBox searchBox;
	ListBox itemList;
	ListSelectionHandler<String> itemHandler;
	private boolean withButton;
	private String[] loadedProbes;
	private Button addButton;
	

	public ProbeSelector(String label, boolean wb) {
		this.withButton = wb;
		
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
		itemList.setSize("100%", "250px");
		itemList.setVisibleItemCount(5);

		itemHandler = new ListSelectionHandler<String>("pathways",
				itemList, false) {
			protected void getUpdates(String item) {
				if (withButton) {
					addButton.setEnabled(false);
				}
				getProbes(item);						
			}
		};
		
		if (withButton) {
			addButton = new Button("Add selected probes >>");
			addButton.addClickHandler(new ClickHandler() {
				public void onClick(ClickEvent e) {					
					probesChanged(loadedProbes);
				}
			});
			addButton.setEnabled(false);
			vp.add(addButton);
		}
	}
	
	/**
	 * This callback should be supplied to the RPC method that retrieves
	 * high level objects for a partial name.
	 * @return
	 */
	public AsyncCallback<String[]> retrieveMatchesCallback() {
		return itemHandler.retrieveCallback(this, true);
	}
	
	/**
	 * This method should obtain the high level objects that correspond to the
	 * partial name. It will be invoked after the user types a partial name
	 * and presses enter.
	 * @param key
	 */
	abstract protected void getMatches(String key);
	
	/**
	 * This callback should be supplied to the RPC methd that retrieves
	 * probes for a selection.
	 * @return
	 */
	public AsyncCallback<String[]> retrieveProbesCallback() {
		return new PendingAsyncCallback<String[]>(this) {
			public void handleFailure(Throwable caught) {
				Window.alert("Unable to get probes.");
				itemHandler.clear();
				addButton.setEnabled(false);
			}

			public void handleSuccess(String[] probes) {
				if (!withButton) {
					probesChanged(probes);
				} else if (probes.length > 0) {
					addButton.setEnabled(true);
					loadedProbes = probes;
				}
			}
		};
	}	
	
	/**
	 * This method should obtain the probes that correspond to the exactly named
	 * high level object. (Will be invoked after the user selects one)
	 * @param item
	 */
	abstract protected void getProbes(String item);
	
	void clear() {
		searchBox.setText("");
		itemHandler.clear();
		loadedProbes = new String[0];
	}
	
}
