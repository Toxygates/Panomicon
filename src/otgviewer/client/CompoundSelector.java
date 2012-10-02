package otgviewer.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import otgviewer.shared.DataFilter;

import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.NoSelectionModel;

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
	
	private CellTable<String> compoundTable;
	private ScrollPanel scrollPanel;
	private VerticalPanel verticalPanel;
	
	private Set<String> selectedCompounds = new HashSet<String>();
	private Column<String, Boolean> selectColumn;
	private ListDataProvider<String> provider = new ListDataProvider<String>();
	
	
	/**
	 * @wbp.parser.constructor
	 */
	public CompoundSelector() {
		this("Compounds");
	}
	
	public CompoundSelector(String heading) {
//		chosenDataFilter = initFilter;
		
		verticalPanel = new VerticalPanel();
		initWidget(verticalPanel);
		verticalPanel.setWidth("100%");
		
		Label lblCompounds = new Label(heading);
		lblCompounds.setStyleName("heading");
		verticalPanel.add(lblCompounds);
		
		scrollPanel = new ScrollPanel();
		verticalPanel.add(scrollPanel);
		scrollPanel.setSize("100%", "400px");
		
		compoundTable = new CellTable<String>();
		scrollPanel.setWidget(compoundTable);
		compoundTable.setSize("100%", "100%");
		compoundTable.setSelectionModel(new NoSelectionModel<String>());
		
		selectColumn = new Column<String, Boolean>(new CheckboxCell()) {
			@Override
			public Boolean getValue(String object) {

				return selectedCompounds.contains(object);				
			}
		};
		selectColumn.setFieldUpdater(new FieldUpdater<String, Boolean>() {
			@Override
			public void update(int index, String object, Boolean value) {
				if (value) {					
					selectedCompounds.add(object);
				} else {
					selectedCompounds.remove(object);
				}
				
				List<String> r = new ArrayList<String>();
				r.addAll(selectedCompounds);
				Collections.sort(r);
				changeCompounds(r);
			}
		});
		
		compoundTable.addColumn(selectColumn, "Selected");
		
		TextColumn<String> textColumn = new TextColumn<String>() {
			@Override
			public String getValue(String object) {
				return object;
			}
		};
		compoundTable.addColumn(textColumn, "Compound");
		
		provider.addDataDisplay(compoundTable);		
	}
	
	@Override
	public void dataFilterChanged(DataFilter filter) {
		super.dataFilterChanged(filter);		
		loadCompounds();
		selectedCompounds.clear();
		
		//reset any edits the user might have done
		for (String item: provider.getList()) {
			((CheckboxCell) selectColumn.getCell()).clearViewData(provider.getKey(item));
		}
	}
	
	public List<String> getCompounds() {				
		List<String> r = new ArrayList<String>();
		r.addAll(selectedCompounds);		
		Collections.sort(r);
		return r;
	}

	void loadCompounds() {		
		owlimService.compounds(chosenDataFilter, new AsyncCallback<String[]>() {
			
			@Override
			public void onSuccess(String[] result) {
				Arrays.sort(result);
				List<String> r = new ArrayList<String>((Arrays.asList(result)));				
				provider.setList(r);
				compoundTable.setVisibleRange(0, r.size());
			}
			
			@Override
			public void onFailure(Throwable caught) {
				Window.alert("Unable to retrieve compounds");			
			}
		});
	}
	
	public void setSelection(List<String> compounds) {		
		selectedCompounds.clear();
		selectedCompounds.addAll(compounds);
		
		//reset any edits the user might have done
		for (String item: provider.getList()) {
			((CheckboxCell) selectColumn.getCell()).clearViewData(provider.getKey(item));
		}
		
		compoundTable.redraw();		
		Collections.sort(compounds);
		changeCompounds(compounds);
	}
	
	int lastHeight = 0;
	@Override
	public void heightChanged(int newHeight) {
		if (newHeight != lastHeight) {
			lastHeight = newHeight;
			super.heightChanged(newHeight);
			verticalPanel.setHeight((newHeight - verticalPanel.getAbsoluteTop() - 50) + "px");
			scrollPanel.setHeight((newHeight - scrollPanel.getAbsoluteTop() - 70) + "px");
		}
	}
}
