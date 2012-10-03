package otgviewer.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import otgviewer.shared.Barcode;
import otgviewer.shared.DataColumn;
import otgviewer.shared.DataFilter;
import otgviewer.shared.Group;
import otgviewer.shared.SharedUtils;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * This widget is intended to help visually define and modify "groups"
 * of microarrays.
 * 
 * Receives: dataFilter, compounds
 * Emits: columns
 * @author johan
 *
 */
public class GroupInspector extends DataListenerWidget {
	
	private OwlimServiceAsync owlimService = (OwlimServiceAsync) GWT
			.create(OwlimService.class);

	private Grid grid = new Grid();
	private String[] availableTimes;
	private CheckBox[][] checkboxes;
	private Map<String, Group> groups = new HashMap<String, Group>();	
	
	private TextBox txtbxGroup;
	private ListBox existingGroupsList;
	private CompoundSelector compoundSel;
	
	public GroupInspector(CompoundSelector cs) {
		compoundSel = cs;
		VerticalPanel vp = new VerticalPanel();
		vp.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		initWidget(vp);
		
		Label lblGroupDefinition = new Label("2. Group definition");
		lblGroupDefinition.setStyleName("heading");
		vp.add(lblGroupDefinition);
		
		HorizontalPanel horizontalPanel_1 = new HorizontalPanel();
		horizontalPanel_1.setStyleName("slightlySpaced");
		vp.add(horizontalPanel_1);
		
		Button btnSelectAll = new Button("Select all");
		horizontalPanel_1.add(btnSelectAll);
		btnSelectAll.addClickHandler(new ClickHandler(){
			public void onClick(ClickEvent ce) {
				drawGridInner(true);
			}
		});
		
		Button btnSelectNone = new Button("Select none");
		horizontalPanel_1.add(btnSelectNone);
		btnSelectNone.addClickHandler(new ClickHandler(){
			public void onClick(ClickEvent ce) {
				drawGridInner(false);
			}
		});
		
		grid.setStyleName("highlySpaced");
		grid.setWidth("700px");
		grid.setHeight("400px");
		grid.setBorderWidth(0);
		vp.add(grid);
		vp.setWidth("410px");		
		
		HorizontalPanel horizontalPanel = new HorizontalPanel();
		horizontalPanel.setStyleName("slightlySpaced");
		horizontalPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		horizontalPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
		vp.add(horizontalPanel);
		
		Label lblSaveGroupAs = new Label("Save group as");
		lblSaveGroupAs.setStyleName("slightlySpaced");
		horizontalPanel.add(lblSaveGroupAs);
		
		
		txtbxGroup = new TextBox();
		txtbxGroup.setText(nextGroupName());
		horizontalPanel.add(txtbxGroup);
		
		Button btnSave = new Button("Save");
		horizontalPanel.add(btnSave);
		
		Button btnDelete = new Button("Delete");
		horizontalPanel.add(btnDelete);
		
		Label lblDefinedGroups = new Label("3. Existing groups");
		lblDefinedGroups.setStyleName("heading");
		vp.add(lblDefinedGroups);
		
		existingGroupsList = new ListBox();
		vp.add(existingGroupsList);
		existingGroupsList.setSize("100%", "100px");
		existingGroupsList.setVisibleItemCount(5);
		btnSave.addClickHandler(new ClickHandler(){
			public void onClick(ClickEvent ce) {
				makeGroup(txtbxGroup.getValue());				
			}
		});
		
		btnDelete.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent ce) {
				String grp = txtbxGroup.getValue();
				if (groups.containsKey(grp)) {
					groups.remove(grp);
					
					existingGroupsList.clear();
					for (Group g: groups.values()) {
						existingGroupsList.addItem(g.getName());
					}
					
					reflectGroupChanges();
				}
			}
		});
		
		existingGroupsList.addChangeHandler(new ChangeHandler() {
			public void onChange(ChangeEvent ce) {
				int index = existingGroupsList.getSelectedIndex();
				if (index != -1) {
					String name = existingGroupsList.getItemText(index);
					displayGroup(name);
				}
			}
		});
		
	}
	
	private void reflectGroupChanges() {
		chosenColumns = new ArrayList<DataColumn>(groups.values());
		storeColumns();
		txtbxGroup.setText(nextGroupName());
	}
	
	private String nextGroupName() {
		int i = 1;
		String name = "Group " + i;
		while (groups.containsKey(name)) {
			i += 1;
			name = "Group " + i;
		}
		return name;
	}
	
	private void lazyFetchTimes() {
		if (availableTimes != null && availableTimes.length > 0) {
			drawGridInner(false);
		} else {
			fetchTimes();						
		}		
	}
	
	private void fetchTimes() {		
		owlimService.times(chosenDataFilter, null, new AsyncCallback<String[]>() {
			public void onSuccess(String[] times) {
				availableTimes = times;
				drawGridInner(false);
				//TODO: block compound selection until we have obtained this data
			}
			public void onFailure(Throwable caught) {
				Window.alert("Unable to get sample times.");
			}
		});			
	}
	
	@Override
	public void dataFilterChanged(DataFilter filter) {		
		if (!filter.equals(chosenDataFilter)) {			
			super.dataFilterChanged(filter); //this call changes chosenDataFilter			
			availableTimes = null;
			groups.clear();
			existingGroupsList.clear();
			fetchTimes();
			compoundsChanged(new ArrayList<String>());
		} else {
			super.dataFilterChanged(filter);
		}
	}
	
	@Override
	public void compoundsChanged(List<String> compounds) {
		super.compoundsChanged(compounds);		
		redrawGrid();
	}
	
	@Override 
	public void columnsChanged(List<DataColumn> columns) {
		super.columnsChanged(columns);
		groups.clear();
		existingGroupsList.clear();
		//Currently we expect all groups here		
		for (DataColumn c: columns) {
			Group g = (Group) c;
			groups.put(g.getName(), g);
			existingGroupsList.addItem(g.getName());			
		}
		txtbxGroup.setText(nextGroupName());
	}
	
	private void redrawGrid() {
		grid.resize(chosenCompounds.size() + 1, 4);
		
		for (int i = 1; i < chosenCompounds.size() + 1; ++i) {
			Label l = new Label(chosenCompounds.get(i - 1));
			l.setStyleName("emphasized");
			grid.setWidget(i, 0, l);
		}
		
		Label l = new Label("Low");
		l.setStyleName("emphasized");
		grid.setWidget(0, 1, l);
		l = new Label("Medium");
		l.setStyleName("emphasized");
		grid.setWidget(0, 2, l);
		l = new Label("High");
		l.setStyleName("emphasized");
		grid.setWidget(0, 3, l);
		
		
		grid.setHeight(50 * (chosenCompounds.size() + 1) + "px");
		lazyFetchTimes();
	}
	
	private void drawGridInner(boolean initState) {
		checkboxes = new CheckBox[chosenCompounds.size()][];
		
		for (int c = 0; c < chosenCompounds.size(); ++c) {
			checkboxes[c] = new CheckBox[3 * availableTimes.length];
			for (int d = 0; d < 3; ++d) {
				HorizontalPanel hp = new HorizontalPanel();
				for (int t = 0; t < availableTimes.length; ++t) {				
					CheckBox cb = new CheckBox(availableTimes[t]);
					cb.setValue(initState);					
					checkboxes[c][availableTimes.length * d + t] = cb;
					hp.add(cb);					
				}
				CheckBox all = new CheckBox("All");
				all.addValueChangeHandler(new MultiSelectHandler(c, availableTimes.length * d, availableTimes.length * (d + 1)));
				hp.add(all);
				
				SimplePanel sp = new SimplePanel(hp);
				sp.setStyleName("border");					
				grid.setWidget(c + 1, d + 1, sp);					
			}
		}
	}
	
	public Map<String, Group> getGroups() {
		return groups;
	}
	
//	Group pendingGroup;
	
	/**
	 * Get here if save button is clicked
	 * @param name
	 */
	private void makeGroup(final String name) {
		
		if (!groups.containsKey(name)) {
			existingGroupsList.addItem(name);
		}
		Group pendingGroup = new Group(name, new Barcode[0]);
		groups.put(name, pendingGroup);
		
		for (int c = 0; c < chosenCompounds.size(); ++c) {
			for (int d = 0; d < 3; ++d) {
				for (int t = 0; t < availableTimes.length; ++t) {
					if (checkboxes[c][availableTimes.length * d + t].getValue()) {
						final String compound = chosenCompounds.get(c);
						final String dose = indexToDose(d);
						
						final String time = availableTimes[t];
						
						owlimService.barcodes(chosenDataFilter, compound,
								dose, time,
								new AsyncCallback<Barcode[]>() {
									public void onSuccess(Barcode[] barcodes) {
										addToGroup(name, compound + "/" + dose + "/" + time, barcodes);
									}

									public void onFailure(Throwable caught) {
										Window.alert("Unable to retrieve barcodes for the group definition.");
									}
								});
					}
				}
			}
		}		
	}
	
	private int doseToIndex(String dose) {
		if (dose.equals("Low")) {
			return 0;
		} else if (dose.equals("Middle")) {
			return 1;
		} else {
			return 2;
		}
	}
	
	private String indexToDose(int dose) {
		switch (dose) {
		case 0:
			return "Low";			
		case 1:
			return "Middle";								
		}
		return "High";
	}
	
	private void addToGroup(String pendingGroupName, String humanReadable, Barcode[] barcodes) {
		
		if (barcodes.length == 0) {
			Window.alert("No samples were found for: " + humanReadable);
		} else {
			List<Barcode> n = new ArrayList<Barcode>();
			Group pendingGroup = groups.get(pendingGroupName);
			n.addAll(Arrays.asList(barcodes));
			n.addAll(Arrays.asList(pendingGroup.getBarcodes()));
			pendingGroup = new Group(pendingGroupName,
					n.toArray(new Barcode[0]));
			groups.put(pendingGroupName, pendingGroup);

			reflectGroupChanges();
		}
	}
	
	private void displayGroup(String name) {
		List<String> compounds = new ArrayList<String>(Arrays.asList(groups.get(name).getCompounds()));
		
		compoundSel.setSelection(compounds);		
		txtbxGroup.setValue(name);
		
		Group g = groups.get(name);
		//set the appropriate checkboxes
		for (Barcode b: g.getBarcodes()) {
			String c = b.getCompound();
			int ci = SharedUtils.indexOf(chosenCompounds, c);
			int time = SharedUtils.indexOf(availableTimes, b.getTime());
			int dose = doseToIndex(b.getDose());			
			checkboxes[ci][dose * availableTimes.length + time].setValue(true);			
		}		
	}
	
	private class MultiSelectHandler implements ValueChangeHandler<Boolean> {
		private int from, to, row;
		MultiSelectHandler(int row, int from, int to) {
			this.from = from;
			this.to = to;
			this.row = row;
		}
		
		public void onValueChange(ValueChangeEvent<Boolean> vce) {
			for (int i = from; i < to; ++i) {
				checkboxes[row][i].setValue(vce.getValue());
			}
		}
	}
}
