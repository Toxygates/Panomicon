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
import otgviewer.client.components.SelectionTable;
import otgviewer.shared.Barcode;
import otgviewer.shared.DataColumn;
import otgviewer.shared.DataFilter;
import otgviewer.shared.Group;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.view.client.SelectionChangeEvent;

/**
 * This widget is intended to help visually define and modify "groups"
 * of microarrays.
 * 
 * Receives: dataFilter, compounds
 * Emits: columns
 * @author johan
 *
 */
public class GroupInspector extends DataListenerWidget implements TimeDoseGrid.BarcodeListener {

	private TimeDoseGrid timeDoseGrid;
	private Map<String, Group> groups = new HashMap<String, Group>();		
	
	private TextBox txtbxGroup;
	SelectionTable<Group> existingGroupsTable;
	private CompoundSelector compoundSel;

	public GroupInspector(CompoundSelector cs) {
		compoundSel = cs;
		VerticalPanel vp = new VerticalPanel();
		vp.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		initWidget(vp);
		
		Label lblGroupDefinition = new Label("Sample group definition");
		lblGroupDefinition.setStyleName("heading");
		vp.add(lblGroupDefinition);
		
		timeDoseGrid = new TimeDoseGrid();
		vp.add(timeDoseGrid);
		addListener(timeDoseGrid);
	
		vp.setWidth("410px");		
		
		HorizontalPanel horizontalPanel = Utils.mkHorizontalPanel();
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
		
		
		existingGroupsTable = new SelectionTable<Group>("Active") {
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
						return "" + object.getBarcodes().length;
					}
				};
				table.addColumn(textColumn, "Samples");
			}
			
			protected void selectionChanged(Set<Group> selected) {
				chosenColumns = new ArrayList<DataColumn>(selected);
				storeColumns();
			}
		};
		vp.add(existingGroupsTable);
		existingGroupsTable.setSize("100%", "100px");
		
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
					reflectGroupChanges();
				}
			}
		});
		
		existingGroupsTable.table().getSelectionModel().addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
			@Override
			public void onSelectionChange(SelectionChangeEvent event) {
				Group g = existingGroupsTable.highlightedRow();
				if (g != null) {
					displayGroup(g.getName());
				}
			}
		});		
	}

	private List<Group> sortedGroupList(Collection<Group> groups) {
		ArrayList<Group> r = new ArrayList<Group>(groups);
		Collections.sort(r);
		return r;
	}
	
	private void reflectGroupChanges() {
		chosenColumns = new ArrayList<DataColumn>(existingGroupsTable.selection());
		existingGroupsTable.reloadWith(sortedGroupList(groups.values()), false);
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
	
	@Override
	public void dataFilterChanged(DataFilter filter) {		
		if (!filter.equals(chosenDataFilter)) {			
			super.dataFilterChanged(filter); //this call changes chosenDataFilter						
			groups.clear();
			existingGroupsTable.reloadWith(new ArrayList<Group>(), true);						
			compoundsChanged(new ArrayList<String>());
		} else {
			super.dataFilterChanged(filter);
		}
	}
	
	@Override 
	public void columnsChanged(List<DataColumn> columns) {
		super.columnsChanged(columns);
		groups.clear();
			
		for (DataColumn c: columns) {
			Group g = (Group) c;
			groups.put(g.getName(), g);			
		}
		txtbxGroup.setText(nextGroupName());		
		existingGroupsTable.reloadWith(sortedGroupList(groups.values()), true);
		existingGroupsTable.setSelection(asGroupList(chosenColumns));
	}
	
	public void inactiveColumnsChanged(List<DataColumn> columns) {
		Collection<Group> igs = sortedGroupList(asGroupList(columns));
		for (Group g : igs) {
			groups.put(g.getName(), g);
		}
		txtbxGroup.setText(nextGroupName());
		List<Group> all = new ArrayList<Group>();
		all.addAll(sortedGroupList(existingGroupsTable.selection()));
		all.addAll(igs);
		existingGroupsTable.reloadWith(all, false);		
		existingGroupsTable.unselectAll(igs);
		existingGroupsTable.table().redraw();
	}
	
	private List<Group> asGroupList(Collection<DataColumn> dcs) {
		List<Group> r = new ArrayList<Group>();
		for (DataColumn dc : dcs) {
			r.add((Group) dc);
		}
		return r;
	}
	
	@Override 
	public void storeColumns() {
		super.storeColumns();			
		storeColumns("inactiveColumns", 
				new ArrayList<DataColumn>(existingGroupsTable.inverseSelection()));
	}
	
	public Map<String, Group> getGroups() {
		return groups;
	}
	
	private Group pendingGroup;
	
	/**
	 * Get here if save button is clicked
	 * @param name
	 */
	private void makeGroup(final String name) {
		
		pendingGroup = new Group(name, new Barcode[0]);		
		groups.put(name, pendingGroup);
		existingGroupsTable.addItem(pendingGroup);
		existingGroupsTable.setSelected(pendingGroup);

		timeDoseGrid.getSelection(this);		
	}
	
	public void barcodesObtained(Barcode[] barcodes, String description) {
		addToGroup(pendingGroup.getName(), description, barcodes);
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
			
			existingGroupsTable.removeItem(groups.get(pendingGroupName));
			groups.put(pendingGroupName, pendingGroup);
			existingGroupsTable.addItem(pendingGroup);
			existingGroupsTable.setSelected(pendingGroup);
			reflectGroupChanges();
		}
	}
	
	private void displayGroup(String name) {
		List<String> compounds = new ArrayList<String>(Arrays.asList(groups.get(name).getCompounds()));
		
		compoundSel.setSelection(compounds);		
		txtbxGroup.setValue(name);
		
		Group g = groups.get(name);
		timeDoseGrid.setSelection(g.getBarcodes());				
	}
	
}
