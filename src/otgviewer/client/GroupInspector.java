package otgviewer.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import otgviewer.client.components.DataListenerWidget;
import otgviewer.client.components.GroupMaker;
import otgviewer.client.components.Screen;
import otgviewer.client.components.StorageParser;
import otgviewer.shared.Group;
import otgviewer.shared.OTGColumn;
import otgviewer.shared.OTGSample;
import t.common.client.components.SelectionTable;
import t.common.shared.DataSchema;
import t.common.shared.Pair;
import t.common.shared.SampleClass;
import t.common.shared.SharedUtils;
import t.common.shared.Unit;

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
 * This widget is intended to help visually define and modify groups
 * of samples.
 * The main dose/time grid is implemented in the SelectionTDGrid. The rest is in this class. 
 */
public class GroupInspector extends DataListenerWidget implements RequiresResize, SelectionTDGrid.UnitListener { 

	private MultiSelectionGrid msg;
	private Map<String, Group> groups = new HashMap<String, Group>();		
	private final Screen screen;
	private final DataSchema schema;
	private Label titleLabel;
	private TextBox txtbxGroup;
	private Button saveButton, autoGroupsButton;
	SelectionTable<Group> existingGroupsTable;
	private CompoundSelector compoundSel;
	private HorizontalPanel toolPanel;
	private SplitLayoutPanel sp;
	private VerticalPanel vp;
	private boolean nameIsAutoGen = false;
	
	private List<Pair<Unit, Unit>> availableUnits;

	protected final Logger logger = Utils.getLogger("group");
	
	public GroupInspector(CompoundSelector cs, Screen scr) {
		compoundSel = cs;
		this.screen = scr;
		this.schema = scr.schema();
		sp = new SplitLayoutPanel();
		initWidget(sp);

		vp = Utils.mkTallPanel();

		titleLabel = new Label("Sample group definition");
		titleLabel.setStyleName("heading");
		vp.add(titleLabel);
		
		msg = new MultiSelectionGrid(scr, this);
		vp.add(msg);
		addListener(msg);
		
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
		
		autoGroupsButton = new Button("Automatic groups",
				new ClickHandler() {
			public void onClick(ClickEvent ce) {
				makeAutoGroups();
			}			
		});
		toolPanel.add(autoGroupsButton);
		
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
				updateConfigureStatus(true);
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
	public void unitsChanged(DataListenerWidget sender, List<Unit> selectedUnits) {
		if (txtbxGroup.getText().equals("") || nameIsAutoGen) {
			txtbxGroup.setText(suggestGroupName(selectedUnits));
			nameIsAutoGen = true;
		}
	}
	
	@Override
	public void availableUnitsChanged(DataListenerWidget sender, List<Pair<Unit, Unit>> units) {
		availableUnits = units;
//		if (availableUnits.size() > 0) {
//			autoGroupsButton.setEnabled(true);
//		} else {
//			autoGroupsButton.setEnabled(false);
//		}
	}
	
	private void deleteGroup(String name, boolean createNew) {
		groups.remove(name);									
		reflectGroupChanges(); //stores columns
		if (createNew) {
			newGroup();
		}
	}
	
	void confirmDeleteAllGroups() {
		int n = existingGroupsTable.getItems().size();
		if (Window.confirm("Delete " + n + " groups?")) {
			groups.clear();
			reflectGroupChanges();
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
		msg.setAll(false);
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
		chosenColumns = new ArrayList<Group>(existingGroupsTable.getSelection());
		logger.info(chosenColumns.size() + " columns have been chosen");
		StorageParser p = getParser(screen);		
		storeColumns(p);
		txtbxGroup.setText("");		
		updateConfigureStatus(true);
		existingGroupsTable.setVisible(groups.values().size() > 0);
		
	}
	
	private void updateConfigureStatus(boolean internalTriggered) {		
		if (chosenColumns.size() == 0) {
			screen.setConfigured(false);			
		} else if (chosenColumns.size() > 0) {
			screen.setConfigured(true);			
		}
		if (internalTriggered) {
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
	
	public String suggestGroupName(List<Unit> units) {
		String g = "";
		if (!units.isEmpty()) {
			Unit b = units.get(0);
			g = firstChars(b.get(schema.majorParameter())) + "/" + 
					b.get(schema.mediumParameter()).substring(0, 1) + "/" + 
					b.get(schema.minorParameter());
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
	public void sampleClassChanged(SampleClass sc) {		
		if (!sc.equals(chosenSampleClass)) {			
			super.sampleClassChanged(sc); //this call changes chosenDataFilter						
//			groups.clear();
//			existingGroupsTable.setItems(new ArrayList<Group>(), true);			
			compoundsChanged(new ArrayList<String>());
//			newGroup();
		} else {
			super.sampleClassChanged(sc);
		}
	}
	
	@Override 
	public void columnsChanged(List<Group> columns) {
		super.columnsChanged(columns);
		groups.clear();
			
		for (Group g: columns) {			
			groups.put(g.getName(), g);			
		}
		updateConfigureStatus(false);
				
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
	
	protected void inactiveColumnsChanged(List<Group> columns) {
		Collection<Group> igs = sortedGroupList(columns);
		for (Group g : igs) {
			groups.put(g.getName(), g);
		}
		
		List<Group> all = new ArrayList<Group>();
		all.addAll(sortedGroupList(existingGroupsTable.getSelection()));
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
				new ArrayList<OTGColumn>(existingGroupsTable.inverseSelection()));
	}
	
	public Map<String, Group> getGroups() {
		return groups;
	}
	
	private void makeAutoGroups() {
		List<Group> gs = GroupMaker.autoGroups(this, schema, availableUnits);
		for (Group g: gs) {
			addGroup(g);
		}
		reflectGroupChanges();
	}
	
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
				
		List<Unit> units = msg.fullSelection(false);
		
		if (units.size() == 0) {
			 Window.alert("No samples found.");			 
		} else {
			setGroup(name, units);
			newGroup();
		}
		
		loadTimeWarningIfNeeded();		
	}
	
	private void loadTimeWarningIfNeeded() {
		int totalSize = 0;		
		for (Group g : existingGroupsTable.getSelection()) {			
			for (OTGSample b: g.samples()) {
				if (!schema.isSelectionControl(b.sampleClass())) {				
					totalSize += 1;
				}
			}
		}
		
		// Conservatively estimate that we load 4 samples per second
		int loadTime = (int) (totalSize / 4);

		if (loadTime > 20) {
			Window.alert("Warning: You have requested data for " + totalSize + " samples.\n" +
					"The total loading time is expected to be " + loadTime + " seconds.");
		}
	} 

	private void setGroup(String pendingGroupName, List<Unit> units) {
		logger.info("Set group with " + SharedUtils.mkString(units, ","));
		Group pendingGroup = groups.get(pendingGroupName);
		existingGroupsTable.removeItem(pendingGroup); 
		pendingGroup = new Group(schema, pendingGroupName, units.toArray(new Unit[0]));
		addGroup(pendingGroup);
		reflectGroupChanges();
	}
	
	private void addGroup(Group group) {
		String name = group.getName();
		groups.put(name, group);
		logger.info("Add group " + name + " with " + group.getSamples().length + " samples " +
				"and " + group.getUnits().length + " units ");
		
		existingGroupsTable.addItem(group);
		existingGroupsTable.setSelected(group);
	}

	private void displayGroup(String name) {
		setHeading("editing " + name);
		
		List<String> compounds = new ArrayList<String>(
				groups.get(name).getMajors(chosenSampleClass));
		
		compoundSel.setSelection(compounds);		
		txtbxGroup.setValue(name);
		nameIsAutoGen = false;
		
		Group g = groups.get(name);
		msg.setSelection(g.getUnits());
		
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
 