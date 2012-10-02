package otgviewer.client;

import java.util.List;

import otgviewer.shared.DataFilter;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Label;

/**
 * This widget is for selecting a compound or a set of 
 * compounds using various data sources.
 * 
 * Receives: dataFilter
 * Emits: compounds
 * @author johan
 *
 */
public class CompoundSelector extends DataListenerWidget {

	private OwlimServiceAsync owlimService = (OwlimServiceAsync) GWT
			.create(OwlimService.class);
	private MultiSelectionHandler<String> compoundHandler;		
	
	public CompoundSelector(String heading) {
//		chosenDataFilter = initFilter;
		
		VerticalPanel verticalPanel = new VerticalPanel();
		initWidget(verticalPanel);
		verticalPanel.setWidth("100%");
		
		Label lblCompounds = new Label(heading);
		lblCompounds.setStyleName("heading");
		verticalPanel.add(lblCompounds);
		
		ListBox compoundList = new ListBox();
		compoundList.setVisibleItemCount(10);
		verticalPanel.add(compoundList);
		compoundList.setSize("100%", "400px");
		compoundList.setMultipleSelect(true);

		compoundHandler = new MultiSelectionHandler<String>("compounds",
				compoundList) {
			protected void getUpdates(List<String> compounds) {
				changeCompounds(compounds);				
			}
		};
		
	}
	
	@Override
	public void dataFilterChanged(DataFilter filter) {
		super.dataFilterChanged(filter);		
		loadCompounds();
	}
	
	public List<String> getCompounds() {
		return compoundHandler.lastMultiSelection();
	}

	void loadCompounds() {
		owlimService.compounds(chosenDataFilter, compoundHandler.retrieveCallback());
	}
	
	public void setSelection(List<String> compounds) {
		compoundHandler.setSelection(compounds);
		changeCompounds(compounds);
	}
}
