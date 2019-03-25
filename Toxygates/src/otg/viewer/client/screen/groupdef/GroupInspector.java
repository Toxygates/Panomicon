/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health
 * and Nutrition (NIBIOHN), Japan.
 * 
 * This file is part of Toxygates.
 * 
 * Toxygates is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * Toxygates is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Toxygates. If not,
 * see <http://www.gnu.org/licenses/>.
 */

package otg.viewer.client.screen.groupdef;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gwt.cell.client.ButtonCellBase;
import com.google.gwt.cell.client.ButtonCellBase.DefaultAppearance.Style;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.RowStyles;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;

import otg.model.sample.OTGAttribute;
import otg.viewer.client.components.OTGScreen;
import t.common.client.components.SelectionTable;
import t.common.shared.*;
import t.common.shared.sample.*;
import t.model.SampleClass;
import t.viewer.client.*;
import t.viewer.client.components.ImmediateValueChangeTextBox;
import t.viewer.client.future.Future;
import t.viewer.client.storage.StorageProvider;

/**
 * This widget provides a visual interface where the user can interactively
 * define and modify groups of samples. The main dose/time grid is implemented
 * in the SelectionTDGrid. The rest is in this class.
 */
abstract public class GroupInspector extends Composite implements RequiresResize,
    SelectionTDGrid.UnitListener, ExistingGroupsTable.Delegate {

  public final Groups groups = new Groups();

  public MultiSelectionGrid multiSelectionGrid;
  private final OTGScreen screen;
  private final Delegate delegate;
  private final DataSchema schema;
  private Label titleLabel;
  private TextBox txtbxGroup;
  private Button saveButton, autoGroupsButton;
  private SelectionTable<Group> existingGroupsTable;
  private HorizontalPanel toolPanel;
  private SplitLayoutPanel splitPanel;
  private VerticalPanel verticalPanel;
  private boolean nameIsAutoGen = false;

  private List<Pair<Unit, Unit>> availableUnits;

  protected final Logger logger = SharedUtils.getLogger("group");

  protected List<Dataset> chosenDatasets = new ArrayList<Dataset>();
  protected List<String> chosenCompounds = new ArrayList<String>();

  public interface Delegate {
    Future<SampleClass[]> enableDatasetsIfNeeded(Collection<Group> groups);
    void groupInspectorEditGroup(Group group, SampleClass sampleClass, List<String> compounds);
    void groupInspectorDatasetsChanged(List<Dataset> ds);
    void groupInspectorClearCompounds();
  }

  public interface ButtonCellResources extends ButtonCellBase.DefaultAppearance.Resources {
    @Override
    @Source("otg/viewer/client/ButtonCellBase.gss")
    Style buttonCellBaseStyle();
  }

  public GroupInspector(OTGScreen scr, Delegate delegate) {
    this.screen = scr;
    this.delegate = delegate;
    schema = scr.schema();
    splitPanel = new SplitLayoutPanel();
    initWidget(splitPanel);

    verticalPanel = Utils.mkTallPanel();

    titleLabel = new Label("Sample group definition");
    titleLabel.addStyleName("heading");
    verticalPanel.add(titleLabel);

    multiSelectionGrid = new MultiSelectionGrid(scr, this);
    verticalPanel.add(multiSelectionGrid);

    verticalPanel.setWidth("440px");

    toolPanel = Utils.mkHorizontalPanel(true);
    verticalPanel.add(toolPanel);

    Label lblSaveGroupAs = new Label("Save group as");
    lblSaveGroupAs.addStyleName("slightlySpaced");
    toolPanel.add(lblSaveGroupAs);

    txtbxGroup = new ImmediateValueChangeTextBox();
    txtbxGroup.addValueChangeHandler(new ValueChangeHandler<String>() {
      @Override
      public void onValueChange(ValueChangeEvent<String> event) {
        nameIsAutoGen = false;
        onGroupNameInputChanged();
      }
    });
    txtbxGroup.addKeyDownHandler(new KeyDownHandler() {
      // Pressing enter saves a group, but only if saving a new group rather than overwriting.
      @Override
      public void onKeyDown(KeyDownEvent event) {
        if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER && !groups.containsKey(txtbxGroup.getValue())) {
          makeGroup(txtbxGroup.getValue());
        }
      }
    });
    toolPanel.add(txtbxGroup);

    saveButton = new Button("Save", (ClickHandler) e -> {
      makeGroup(txtbxGroup.getValue());
    });
    toolPanel.add(saveButton);

    autoGroupsButton = new Button("Automatic groups", (ClickHandler) e -> {
      makeAutoGroups();      
    });
    toolPanel.add(autoGroupsButton);

    setEditMode(false);

    existingGroupsTable = new ExistingGroupsTable(this);
    existingGroupsTable.setVisible(false);
    existingGroupsTable.table().setRowStyles(new GroupColouring());
    existingGroupsTable.setSize("100%", "100px");
    splitPanel.addSouth(t.common.client.Utils.makeScrolled(existingGroupsTable), 200);

    splitPanel.add(t.common.client.Utils.makeScrolled(verticalPanel));
  }
  
  public void loadGroups() {
    groups.loadGroups(screen.getStorage());
    enableDatasetsIfNeeded();
    updateConfigureStatus(false);

    // Reflect loaded group information in UI
    updateTableData();
    existingGroupsTable.table().redraw();
  }

  private void onGroupNameInputChanged() {
    if (groups.containsKey(txtbxGroup.getValue())) {
      saveButton.setText("Overwrite");
    } else {
      saveButton.setText("Save");
    }
  }

  /**
   * Callback from SelectionTDGrid
   */
  @Override
  public void unitsChanged(List<Unit> selectedUnits) {
    if (selectedUnits.isEmpty() && nameIsAutoGen) {
      //retract the previous suggestion
      txtbxGroup.setText("");
    } else if (txtbxGroup.getText().equals("") || nameIsAutoGen) {
      txtbxGroup.setText(groups.suggestName(selectedUnits, schema));
      nameIsAutoGen = true;
    }
    onGroupNameInputChanged();
  }

  @Override
  public void availableUnitsChanged(List<Pair<Unit, Unit>> units) {
    availableUnits = units;
  }
  
  public void initializeState(List<Dataset> datasets, SampleClass sc, 
      List<String> compounds) {
    chosenDatasets = datasets;
    chosenCompounds = compounds;
    if (compounds.size() == 0) {
      setEditMode(false);
    } else {
      setEditMode(true);
    }
    multiSelectionGrid.initializeState(sc, compounds);
  }

  public void datasetsChanged(List<Dataset> datasets) {
    chosenDatasets = datasets;
    disableGroupsIfNeeded(datasets);
  }
  
  public void setCompounds(List<String> compounds) {
    chosenCompounds = compounds;
    if (compounds.size() == 0) {
      setEditMode(false);
    } else {
      setEditMode(true);
    }
    multiSelectionGrid.setCompounds(compounds);
  }


  public void confirmDeleteAllGroups() {
    if (Window.confirm("Delete " + groups.size() + " groups?")) {
      groups.clear();
      reflectGroupChanges();
      clearUiForNewGroup();
    }
  }

  private void setEditMode(boolean editing) {
    boolean val = editing && (chosenCompounds.size() > 0);
    toolPanel.setVisible(val);
  }

  private void setHeading(String title) {
    titleLabel.setText("Sample group definition - " + title);
  }

  /**
   * Clears selections and input fields in the UI to prepare for the user entering
   * information for a new group.
   */
  private void clearUiForNewGroup() {
    txtbxGroup.setText("");
    onGroupNameInputChanged();
    multiSelectionGrid.clearSelection();
    delegate.groupInspectorClearCompounds();
    setHeading("new group");
    setEditMode(true);
  }

  /**
   * To be called whenever the set of active, or inactive groups, changes, in
   * order to make corresponding updates to the UI.
   * 
   * @param store whether changes to the active and inactive group sets should be
   *          saved to local storage
   */
  private void reflectGroupChanges() {
    groups.saveToLocalStorage(screen.getStorage());
    txtbxGroup.setText("");
    updateConfigureStatus(true);
    updateTableData();
  }

  /**
   * Update some application state based on the currently active groups. Called
   * after each change to the set of active groups.
   */
  private void updateConfigureStatus(boolean triggeredByUserAction) {
    if (triggeredByUserAction) {
      screen.manager().resetWorkflowLinks();
    }
  }

  protected void disableGroupsIfNeeded(List<Dataset> datasets) {
    Set<String> availableDatasets = new HashSet<String>();
    for (Dataset d : datasets) {
      availableDatasets.add(d.getId());
    }
    int disableCount = 0;

    logger.info("Available datasets: " + SharedUtils.mkString(availableDatasets, ", "));
    for (Group group : new ArrayList<Group>(groups.activeGroups())) {
      List<String> requiredDatasets = group.collect(OTGAttribute.Dataset).collect(Collectors.toList());
      logger.info("Group " + group.getShortTitle() + " needs " + SharedUtils.mkString(requiredDatasets, ", "));
      if (!availableDatasets.containsAll(requiredDatasets)) {
        groups.deactivate(group);
        disableCount += 1;
      }
    }
    if (disableCount > 0) {
      reflectGroupChanges();
      Window
          .alert(disableCount + " group(s) were deactivated " + "because of your dataset choice.");
    }
  }

  protected void enableDatasetsIfNeeded() {
    delegate.enableDatasetsIfNeeded(groups.activeGroups());
  }

  private void updateTableData() {
    existingGroupsTable.setItems(groups.allGroups(), false);
    existingGroupsTable.setSelection(groups.activeGroups());
    existingGroupsTable.setVisible(groups.size() > 0);
  }

  private void makeAutoGroups() {
    List<Group> gs = GroupMaker.autoGroups(this, schema, availableUnits);
    for (Group g : gs) {
      addGroup(g, true);
    }
    // TODO investigate whether this enableDatasetsIfNeeded call can be removed
    enableDatasetsIfNeeded(); 
    reflectGroupChanges();
    clearUiForNewGroup();
  }

  /**
   * Get here if save button is clicked
   */
  private void makeGroup(String name) {
    if (name.trim().equals("")) {
      Window.alert("Please enter a group name.");
      return;
    }
    if (!StorageProvider.isAcceptableString(name, "Unacceptable group name.")) {
      return;
    }

    List<Unit> units = multiSelectionGrid.fullSelection(false);

    if (units.size() == 0) {
      Window.alert("No samples found.");
    } else {
      Group newGroup = setGroup(name, units);
      clearUiForNewGroup();
      loadTimeWarningIfNeeded(newGroup);
    }
  }

  private void loadTimeWarningIfNeeded(Group newGroup) {
    Set<String> newIds = Stream.of(newGroup.samples())
      .filter(sample -> !schema.isSelectionControl(sample.sampleClass()))
      .map(sample -> sample.id())
      .collect(Collectors.toSet());
    
    HashSet<String> allIds = groups.activeGroups().stream()
      .flatMap(group -> Stream.of(group.samples()))
      .filter(sample -> !schema.isSelectionControl(sample.sampleClass()))
      .map(sample -> sample.id())
      .collect(Collectors.toCollection(HashSet::new));
    allIds.addAll(newIds);

    if (allIds.size() > 200) { // just an arbitrary cutoff
      Window.alert("Warning: Your new group contains " + newIds.size() + " samples.\n"
          + "You will now be requesting data for " + allIds.size() + " samples.\n"
          + "The total loading time is expected to be very long.");
    }
  }
  
  /**
   * Sets a group's units, and updates it in the existing groups table (or adds it),
   * as well as saving it to the Groups object.  
   */
  private Group setGroup(String pendingGroupName, List<Unit> units) {
    logger.info("Set group with " + SharedUtils.mkString(units, ","));
    Group existingGroup = groups.get(pendingGroupName);
    if (existingGroup == null) {
      Analytics.trackEvent(Analytics.CATEGORY_GENERAL, Analytics.ACTION_CREATE_NEW_SAMPLE_GROUP);
    } else {
      existingGroupsTable.removeItem(existingGroup);
      Analytics.trackEvent(Analytics.CATEGORY_GENERAL,
          Analytics.ACTION_MODIFY_EXISTING_SAMPLE_GROUP);
    }

    Group newGroup = new Group(schema, pendingGroupName, units.toArray(new Unit[0]));
    addGroup(newGroup, true);
    // TODO: investigate if this enableDatasetsIfNeeded call is necessary
    enableDatasetsIfNeeded();
    reflectGroupChanges();
    return newGroup;
  }

  /**
   * Adds a group to the existing groups table
   */
  private void addGroup(Group group, boolean active) {
    String name = group.getName();
    groups.put(name, group, active);

    logger.info("Add group " + name + " with " + group.getSamples().length + " samples " + "and "
        + group.getUnits().length + " units ");
  }

  @Override
  public void onResize() {
    splitPanel.onResize();
  }

  private class GroupColouring implements RowStyles<Group> {
    @Override
    public String getStyleNames(Group g, int rowIndex) {
      return g.getStyleName();
    }
  }
  
  // ExistingGroupsTable.Delegate methods
  @Override
  abstract public void makeGroupColumns(CellTable<Group> table);
  
  @Override
  public void displayGroupForEditing(String name) {
    setHeading("editing " + name);
    txtbxGroup.setValue(name);
    onGroupNameInputChanged();
    nameIsAutoGen = false;
    
    Group group = groups.get(name);
    SampleClass sampleClass = 
        SampleClassUtils.asMacroClass(group.getSamples()[0].sampleClass(), schema);
    chosenCompounds = 
        SampleClassUtils.getMajors(schema, groups.get(name), sampleClass).
        collect(Collectors.toList());
    setEditMode(true);
    
    delegate.groupInspectorEditGroup(group, sampleClass, chosenCompounds);
    
    multiSelectionGrid.activateSection(sampleClass);
  }
  
  @Override
  public void deleteGroup(String name) {
    groups.remove(name);
    reflectGroupChanges();
    clearUiForNewGroup();
  }
  
  @Override
  public void existingGroupsTableSelectionChanged(Set<Group> selected) {
    groups.setActiveGroups(selected);
    groups.saveToLocalStorage(screen.getStorage());
    enableDatasetsIfNeeded();
    updateConfigureStatus(true);
  }
}
