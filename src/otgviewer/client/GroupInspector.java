package otgviewer.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import otgviewer.client.components.DataListenerWidget;
import otgviewer.client.components.Screen;
import otgviewer.client.components.SelectionTable;
import otgviewer.client.components.StorageParser;
import otgviewer.shared.BUnit;
import otgviewer.shared.Barcode;
import otgviewer.shared.BarcodeColumn;
import otgviewer.shared.DataFilter;
import otgviewer.shared.Group;

import com.google.gwt.cell.client.ButtonCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.RowStyles;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * This widget is intended to help visually define and modify "groups"
 * of microarrays.
 * The main dose/time grid is implemented in the SelectionTDGrid. The rest is in this class.
 * 
 * Receives: dataFilter, compounds
 * Emits: columns
 * @author johan
 *
 */
public class GroupInspector extends DataListenerWidget implements RequiresResize, SelectionTDGrid.UnitListener { 

	private SelectionTDGrid timeDoseGrid;
	private Map<String, Group> groups = new HashMap<String, Group>();		
	private Screen screen;
	private Label titleLabel;
	private TextBox txtbxGroup;
	private Button saveButton;
	SelectionTable<Group> existingGroupsTable;
	private CompoundSelector compoundSel;
	private HorizontalPanel toolPanel;
	private SplitLayoutPanel sp;
	private boolean nameIsAutoGen = false;
	
	public GroupInspector(CompoundSelector cs, Screen scr) {
		compoundSel = cs;
		this.screen = scr;
		sp = new SplitLayoutPanel();
		initWidget(sp);

		VerticalPanel vp = Utils.mkTallPanel();

		titleLabel = new Label("Sample group definition");
		titleLabel.setStyleName("heading");
		vp.add(titleLabel);
		
		timeDoseGrid = new SelectionTDGrid(scr, this);
		vp.add(timeDoseGrid);
		addListener(timeDoseGrid);
	
		vp.setWidth("440px");		
		
		toolPanel = Utils.mkHorizontalPanel(true);
		vp.add(toolPanel);
		
		Label lblSaveGroupAs = new Label("Save group as");
		lblSaveGroupAs.setStyleName("slightlySpaced");
		toolPanel.add(lblSaveGroupAs);
		
		txtbxGroup = new TextBox();
		txtbxGroup.addValueChangeHandler(new ValueChangeHandler<String>() {			
			@Override
			public void onValueChange(ValueChangeEvent<String> event) {
				nameIsAutoGen = false;				
			}
		});
		toolPanel.add(txtbxGroup);		
		
		saveButton = new Button("Save",
		new ClickHandler(){
			public void onClick(ClickEvent ce) {
				makeGroup(txtbxGroup.getValue());
			}
		});
		toolPanel.add(saveButton);
		setEditing(false);
						
		existingGroupsTable = new SelectionTable<Group>("Active", false) {
			protected void initTable(CellTable<Group> table) {
				TextColumn<Group> textColumn = new TextColumn<Group>() {
					@Override
					public String getValue(Group object) {
						return object.getName();
					}
				};
				table.addColumn(textColumn, "Group");
				
				textColumn = new TextColumn<Group>() {
					@Override
					public String getValue(Group object) {
						return "" + object.getTreatedSamples().length;
					}
				};
				table.addColumn(textColumn, "#Treated samples");
				
				textColumn = new TextColumn<Group>() {
					@Override
					public String getValue(Group object) {
						return "" + object.getControlSamples().length;
					}
				};
				table.addColumn(textColumn, "#Control samples");
				
				ButtonCell editCell = new ButtonCell();
				
				Column<Group, String> editColumn = new Column<Group, String>(editCell) {
					public String getValue(Group g) {
						return "Edit";
					}					
				};
				editColumn.setFieldUpdater(new FieldUpdater<Group,String>() {					
					@Override
					public void update(int index, Group object, String value) {						
						displayGroup(object.getName());							
					}
				});
				table.addColumn(editColumn, "");
				
				ButtonCell deleteCell = new ButtonCell();
				Column<Group, String> deleteColumn = new Column<Group, String>(deleteCell) {
					public String getValue(Group g) {
						return "Delete";
					}
				};
				deleteColumn.setFieldUpdater(new FieldUpdater<Group, String>() {
					@Override
					public void update(int index, Group object, String value) {
						if (Window.confirm("Are you sure you want to delete the group " + object.getName() + "?")) {
							deleteGroup(object.getName(), true);						
						}
					}
					
				});
				table.addColumn(deleteColumn, "");
				
			}
			
			protected void selectionChanged(Set<Group> selected) {
				chosenColumns = new ArrayList<Group>(selected);
				StorageParser p = getParser(screen);
				storeColumns(p);
				updateConfigureStatus();
			}
		};
//		vp.add(existingGroupsTable);
		existingGroupsTable.setVisible(false);
		existingGroupsTable.table().setRowStyles(new GroupColouring());
		existingGroupsTable.setSize("100%", "100px");
		sp.addSouth(Utils.makeScrolled(existingGroupsTable), 200);
		
		sp.add(Utils.makeScrolled(vp));
	}
	
	/**
	 * Callback from SelectionTDGrid
	 * @param selectedUnits
	 */
	@Override
	public void unitsChanged(List<BUnit> selectedUnits) {
		if (txtbxGroup.getText().equals("") || nameIsAutoGen) {
			txtbxGroup.setText(suggestGroupName(selectedUnits));
			nameIsAutoGen = true;
		}
	}
	
	private void deleteGroup(String name, boolean createNew) {
		groups.remove(name);									
		reflectGroupChanges(); //stores columns
		if (createNew) {
			newGroup();
		}
	}
	
	/**
	 * Toggle edit mode
	 * @param editing
	 */
	private void setEditing(boolean editing) {
		boolean val = editing && (chosenCompounds.size() > 0);
		toolPanel.setVisible(val);
	}

	
	private void setHeading(String title) {
		titleLabel.setText("Sample group definition - " + title);
	}

	private void newGroup() {
		txtbxGroup.setText("");
		timeDoseGrid.setAll(false);
		compoundSel.setSelection(new ArrayList<String>());
		setHeading("new group");
		setEditing(true);		
	}
	
	private List<Group> sortedGroupList(Collection<Group> groups) {
		ArrayList<Group> r = new ArrayList<Group>(groups);
		Collections.sort(r);
		return r;
	}
	
	private void reflectGroupChanges() {
		existingGroupsTable.setItems(sortedGroupList(groups.values()), false);
		chosenColumns = new ArrayList<Group>(existingGroupsTable.selection());
		StorageParser p = getParser(screen);		
		storeColumns(p);
		txtbxGroup.setText("");
		updateConfigureStatus();
		existingGroupsTable.setVisible(groups.values().size() > 0);		
	}
	
	private void updateConfigureStatus() {		
		if (chosenColumns.size() == 0) {
			screen.setConfigured(false);
			screen.manager().deconfigureAll(screen);
		} else if (chosenColumns.size() > 0) {
			screen.setConfigured(true);
			screen.manager().deconfigureAll(screen);
		}
	}
	
	private String firstChars(String s) {
		if (s.length() < 8) {
			return s;
		} else {
			return s.substring(0, 8);
		}
	}
	
	private String suggestGroupName(List<BUnit> units) {
		String g = "";
		if (!units.isEmpty()) {
			BUnit b = units.get(0);
			g = firstChars(b.getCompound()) + "/" + 
					b.getDose().substring(0, 1) + "/" + 
					b.getTime();
			if (units.size() > 1) {
				g += ", ...";
			}
		} else {
			g = "Empty group";
		}
		int i = 1;
		String name = g;
		while (groups.containsKey(name)) {
			name = g + " " + i;
			i++;
		}
		return name;
	}
	
	@Override
	public void dataFilterChanged(DataFilter filter) {		
		if (!filter.equals(chosenDataFilter)) {			
			super.dataFilterChanged(filter); //this call changes chosenDataFilter						
			groups.clear();
			existingGroupsTable.setItems(new ArrayList<Group>(), true);						
			compoundsChanged(new ArrayList<String>());
			newGroup();
		} else {
			super.dataFilterChanged(filter);
		}
	}
	
	@Override 
	public void columnsChanged(List<Group> columns) {
		super.columnsChanged(columns);
		groups.clear();
			
		for (Group g: columns) {			
			groups.put(g.getName(), g);			
		}
		updateConfigureStatus();
				
		existingGroupsTable.setItems(sortedGroupList(groups.values()), true);
		existingGroupsTable.setSelection(chosenColumns);		
		existingGroupsTable.setVisible(groups.size() > 0);		
		newGroup();
	}

	@Override
	public void compoundsChanged(List<String> compounds) {
		super.compoundsChanged(compounds);
		if (compounds.size() == 0) {
			setEditing(false);			
		} else {
			setEditing(true);
		}
	}
	
	public void inactiveColumnsChanged(List<Group> columns) {
		Collection<Group> igs = sortedGroupList(columns);
		for (Group g : igs) {
			groups.put(g.getName(), g);
		}
		
		List<Group> all = new ArrayList<Group>();
		all.addAll(sortedGroupList(existingGroupsTable.selection()));
		all.addAll(igs);
		existingGroupsTable.setItems(all, false);		
		existingGroupsTable.unselectAll(igs);
		existingGroupsTable.table().redraw();
		existingGroupsTable.setVisible(groups.size() > 0);
		newGroup();
	}
	
	@Override 
	public void storeColumns(StorageParser p) {
		super.storeColumns(p);			
		storeColumns(p, "inactiveColumns", 
				new ArrayList<BarcodeColumn>(existingGroupsTable.inverseSelection()));
	}
	
	public Map<String, Group> getGroups() {
		return groups;
	}
	
	private Group pendingGroup;
	
	/**
	 * Get here if save button is clicked
	 * @param name
	 */
	private void makeGroup(String name) {
		if (name.trim().equals("")) {
			Window.alert("Please enter a group name.");
			return;
		}
		if (!StorageParser.isAcceptableString(name, "Unacceptable group name.")) {
			return;
		}
		
		pendingGroup = new Group(name, new Barcode[0]);
		addGroup(name, pendingGroup);
		List<BUnit> units = timeDoseGrid.getSelectedUnits(false);
		
		if (units.size() == 0) {
			 Window.alert("No samples found.");
			 cullEmptyGroups();
		} else {
			setGroup(pendingGroup.getName(), units);
			newGroup();
		}
		
		loadTimeWarningIfNeeded();		
	}
	
	private void loadTimeWarningIfNeeded() {
		int totalSize = 0;
		for (Group g : groups.values()) {
			for (Barcode b: g.samples()) {
				if (!b.getDose().equals("Control")) {
					totalSize += 1;
				}
			}
		}
		
		// Conservatively estimate that we need 1.5 s per sample to load data
		int loadTime = (int) ((float) totalSize / 1.5);

		if (loadTime > 20) {
			Window.alert("Warning: You have requested data for " + totalSize + " samples.\n" +
					"The total loading time is expected to be " + loadTime + " seconds.");
		}
	}
	
	private void cullEmptyGroups() {
		// look for empty groups, undo the saving
		// this is needed if we found no barcodes or if the user didn't select
		// any combination
		for (String name : groups.keySet()) {
			Group g = groups.get(name);
			if (g.getSamples().length == 0) {
				deleteGroup(name, false);
			}
		}
	}
		
	private void setGroup(String pendingGroupName, List<BUnit> units) {
		Group pendingGroup = groups.get(pendingGroupName);
		existingGroupsTable.removeItem(pendingGroup); 
		pendingGroup = new Group(pendingGroupName, units.toArray(new BUnit[0]));
		addGroup(pendingGroupName, pendingGroup);
		reflectGroupChanges();
	}
	
	private void addGroup(String name, Group group) {
		groups.put(name, group);
		existingGroupsTable.addItem(group);
		existingGroupsTable.setSelected(group);
	}

	private void displayGroup(String name) {
		setHeading("editing " + name);
		List<String> compounds = new ArrayList<String>(Arrays.asList(groups.get(name).getCompounds()));
		
		compoundSel.setSelection(compounds);		
		txtbxGroup.setValue(name);
		nameIsAutoGen = false;
		
		Group g = groups.get(name);
		timeDoseGrid.setSelection(g.getSamples());
		
		setEditing(true);
	}

	@Override
	public void onResize() {
		sp.onResize();		
	}
	
	private class GroupColouring implements RowStyles<Group> {
		@Override
		public String getStyleNames(Group g, int rowIndex) {
			return g.getStyleName();	
		}
	}
}
 