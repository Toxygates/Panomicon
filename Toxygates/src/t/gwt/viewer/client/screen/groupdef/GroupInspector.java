/*
 * Copyright (c) 2012-2019 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition (NIBIOHN), Japan.
 *
 * This file is part of Toxygates.
 *
 * Toxygates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Toxygates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Toxygates. If not, see <http://www.gnu.org/licenses/>.
 */

package t.gwt.viewer.client.screen.groupdef;

import com.google.gwt.cell.client.ButtonCellBase;
import com.google.gwt.cell.client.ButtonCellBase.DefaultAppearance.Style;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.RowStyles;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import t.gwt.viewer.client.screen.Screen;
import t.gwt.common.client.components.SelectionTable;
import t.shared.common.DataSchema;
import t.shared.common.Dataset;
import t.shared.common.SharedUtils;
import t.shared.common.sample.Group;
import t.shared.common.sample.SampleClassUtils;
import t.shared.common.sample.Unit;
import t.model.SampleClass;
import t.model.sample.OTGAttribute;
import t.gwt.viewer.client.Analytics;
import t.gwt.viewer.client.ClientGroup;
import t.gwt.viewer.client.Groups;
import t.gwt.viewer.client.Utils;
import t.gwt.viewer.client.dialog.DialogPosition;
import t.gwt.viewer.client.dialog.SaveObjectDialog;
import t.gwt.viewer.client.future.Future;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This widget provides a visual interface where the user can interactively
 * define and modify groups of samples. The main dose/time grid is implemented
 * in the SelectionTDGrid. The rest is in this class.
 */
abstract public class GroupInspector extends Composite implements RequiresResize,
        ExistingGroupsTable.Delegate {

  public final Groups groups;

  public SelectionTDGrid selectionGrid;
  private final Screen screen;
  private final Delegate delegate;
  private final DataSchema schema;
  /**
   * Label above the selection grid 
   */
  private Label titleLabel;
  private Button saveButton, saveAsButton, cancelButton, autoGroupsButton;
 
  private SelectionTable<ClientGroup> existingGroupsTable;
  /**
   * Panel with input for naming and saving groups 
   */
  private HorizontalPanel toolPanel;
  /**
   * The top-level panel for this widget
   */
  private SplitLayoutPanel splitPanel;
  /**
   * A panel for the selection grid and related widgets
   */
  private VerticalPanel verticalPanel;

  protected DialogBox groupNameInputDialog;
  protected final Logger logger = SharedUtils.getLogger("group");
  
  private Group currentlyEditingGroup = null;

  public interface Delegate {
    Future<SampleClass[]> enableDatasetsIfNeeded(Collection<ClientGroup> groups);
    void groupInspectorEditGroup(ClientGroup group, SampleClass sampleClass, List<String> compounds);
    void groupInspectorClearCompounds();
  }

  public interface ButtonCellResources extends ButtonCellBase.DefaultAppearance.Resources {
    @Override
    @Source("t/gwt/viewer/client/ButtonCellBase.gss")
    Style buttonCellBaseStyle();
  }

  public GroupInspector(Screen scr, Delegate delegate) {
    this.screen = scr;
    this.delegate = delegate;
    
    groups = new Groups(screen.getStorage().groupsStorage);
    
    schema = scr.schema();
    splitPanel = new SplitLayoutPanel();
    initWidget(splitPanel);

    verticalPanel = Utils.mkTallPanel();
    verticalPanel.setWidth("440px");

    titleLabel = new Label("Sample group definition");
    titleLabel.addStyleName("heading");
    verticalPanel.add(titleLabel);

    selectionGrid = screen.factory().selectionTDGrid(screen);
    verticalPanel.add(selectionGrid);

    toolPanel = Utils.mkHorizontalPanel(true);
    verticalPanel.add(toolPanel);
    
    saveButton = new Button("Save", (ClickHandler) e -> {
      saveOverCurrentGroup();
    });
    saveAsButton = new Button("Save as...", (ClickHandler) e -> {
      showGroupNameDialog();
    });
    cancelButton = new Button("Cancel", (ClickHandler) e -> {
      stopEditingGroup();
    });
    autoGroupsButton = new Button("Automatic groups", (ClickHandler) e -> {
      makeAutoGroups();      
    });
  
    setupToolPanel();
    setEditMode();

    existingGroupsTable = new ExistingGroupsTable(this);
    existingGroupsTable.setVisible(false);
    existingGroupsTable.table().setRowStyles(new GroupColouring());
    existingGroupsTable.setSize("100%", "100px");
    splitPanel.addSouth(t.gwt.common.client.Utils.makeScrolled(existingGroupsTable), 200);

    splitPanel.add(t.gwt.common.client.Utils.makeScrolled(verticalPanel));
  }
  
  private void setupToolPanel() {
    toolPanel.remove(saveButton);
    toolPanel.remove(saveAsButton);
    toolPanel.remove(cancelButton);
    toolPanel.remove(autoGroupsButton);
    
    if (currentlyEditingGroup != null) {
      toolPanel.add(saveButton);
    }
    toolPanel.add(saveAsButton);
    if (currentlyEditingGroup != null) {
      toolPanel.add(cancelButton);
    } else {
      toolPanel.add(autoGroupsButton);
    }
  }
  
  /**
   * Load groups from local storage and display them in the existing groups table.
   */
  public void loadGroups() {
    groups.storage().loadFromStorage();
    enableDatasetsIfNeeded();
    updateConfigureStatus(false);

    // Reflect loaded group information in UI
    updateTableData();
    existingGroupsTable.table().redraw();
  }
  
  /**
   * Set the chosen datasets, sample class, and compounds for this group inspector's
   * selection grid. The datasetsChanged flag will determine whether the selection grid
   * will fetch time points when the sample class hasn't changed.
   */
  public void initializeState(SampleClass sc, List<String> compounds,
                              boolean datasetsChanged) {
    selectionGrid.initializeState(sc, compounds, selectionGrid.getSelectedCombinations(), 
        datasetsChanged).addNonErrorCallback(f -> {
      setEditMode();
    });
  }
  
  /**
   * Notification of dataset change from ColumnScreen. 
   */
  public void datasetsChanged(List<Dataset> datasets) {
    disableGroupsIfNeeded(datasets);
  }
  
  /**
   * Called when compounds are changed in the compound selector. 
   */
  public void setCompounds(List<String> compounds) {
    selectionGrid.setCompounds(compounds).addNonErrorCallback(f -> {
      setEditMode();
    });
  }

  /**
   * Deletes all groups if confirmation is received from the user.
   */
  public void confirmDeleteAllGroups() {
    if (Window.confirm("Delete " + groups.size() + " groups?")) {
      groups.clear();
      reflectGroupChanges();
      clearUiForNewGroup();
    }
  }

  /**
   * Sets the visibility of the tool panel
   * @param editing
   */
  public void setEditMode() {
    toolPanel.setVisible(selectionGrid.chosenCompounds().size() > 0);
  }

  private void setHeading(String title) {
    titleLabel.setText("Sample group definition - " + title);
  }

  private void stopEditingGroup() {
    currentlyEditingGroup = null;
    setEditMode();
    setupToolPanel();
    setHeading("new group");
  }
  
  /**
   * Clears selections and input fields in the UI to prepare for the user entering
   * information for a new group.
   */
  private void clearUiForNewGroup() {
    currentlyEditingGroup = null;
    selectionGrid.setAll(false, true);
    delegate.groupInspectorClearCompounds();
    selectionGrid.setCompounds(new ArrayList<String>()).addNonErrorCallback(f -> {
      setEditMode();
    });
    setupToolPanel();
    setHeading("new group");
  }

  /**
   * To be called whenever the set of active, or inactive groups, changes, in
   * order to make corresponding updates to the UI.
   * 
   * @param store whether changes to the active and inactive group sets should be
   *          saved to local storage
   */
  private void reflectGroupChanges() {
    groups.storage().saveToStorage();
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

  /**
   * Disable any groups that contain samples from disable datasets
   * @param datasets the set of all enabled datasets
   */
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

  /**
   * Tell the delegate to enable all the datasets that contain samples from the 
   * currently active sample groups.
   */
  protected void enableDatasetsIfNeeded() {
    delegate.enableDatasetsIfNeeded(groups.activeGroups());
  }
  
  /**
   * Update the existing groups table to reflect changes in sample groups.
   */
  private void updateTableData() {
    existingGroupsTable.setItems(groups.allGroups(), false);
    existingGroupsTable.setSelection(groups.activeGroups());
    existingGroupsTable.setVisible(groups.size() > 0);
  }

  /**
   * Automatically make groups for each of the active compounds, using some algorithm
   * to select the dose levels and exposure times.
   */
  private void makeAutoGroups() {
    GroupMaker.autoGroups(this, schema, selectionGrid.getAvailableUnits(), groups);
    // TODO investigate whether this enableDatasetsIfNeeded call can be removed
    enableDatasetsIfNeeded(); 
    reflectGroupChanges();
    clearUiForNewGroup();
  }
  
  private void saveOverCurrentGroup() {
    List<Unit> units = selectionGrid.getSelectedUnits(false);
    if (units.size() == 0) {
      Window.alert("No samples found.");
      return;
    }
    
    Group newGroup = setGroup(currentlyEditingGroup.getName(), units);
    clearUiForNewGroup();
    loadTimeWarningIfNeeded(newGroup);  
  }

  /**
   * Display a dialog box to ask the user for a name under which to save a group
   * with the currently selected units in the selection grid. 
   * @param name
   */
  private void showGroupNameDialog() {
    List<Unit> units = selectionGrid.getSelectedUnits(false);
    if (units.size() == 0) {
      Window.alert("No samples found.");
      return;
    }
    
    String initialText = currentlyEditingGroup == null ? 
        groups.suggestName(selectionGrid.getSelectedUnits(true), schema) :
        currentlyEditingGroup.getName();
    
    SaveObjectDialog dialog = new SaveObjectDialog("Please enter a name for the group.",
        initialText, groups.storage(), 
        name -> { 
          Group newGroup = setGroup(name, units);
          clearUiForNewGroup();
          loadTimeWarningIfNeeded(newGroup);  
        },
        () -> { groupNameInputDialog.hide(); });
        
    groupNameInputDialog = Utils.displayInPopup("Name entry", dialog, DialogPosition.Center);
  }

  /**
   * Warn the user if the total number of unique samples in the active groups will be 
   * over 200 after the addition of a group containing newGroupUnits.
   * @param newGroupUnits the units in the sample group the user is about to add
   */
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
          + "Together with your other active groups you will now be requesting data for " 
          + allIds.size() + " samples.\n"
          + "The total loading time is expected to be very long.");
    }
  }
  
  /**
   * Sets a group's units, and updates it in the existing groups table (or adds it),
   * as well as saving it to the Groups object.  
   */
  private Group setGroup(String pendingGroupName, List<Unit> units) {
    logger.info("Set group with " + SharedUtils.mkString(units, ","));
    ClientGroup existingGroup = groups.storage().get(pendingGroupName);
    if (existingGroup == null) {
      Analytics.trackEvent(Analytics.CATEGORY_GENERAL, Analytics.ACTION_CREATE_NEW_SAMPLE_GROUP);
    } else {
      existingGroupsTable.removeItem(existingGroup);
      Analytics.trackEvent(Analytics.CATEGORY_GENERAL,
          Analytics.ACTION_MODIFY_EXISTING_SAMPLE_GROUP);
    }

    ClientGroup newGroup = new ClientGroup(schema, pendingGroupName, units.toArray(new Unit[0]), true,
        groups.nextColor());
    addGroup(newGroup);
    // TODO: investigate if this enableDatasetsIfNeeded call is necessary
    enableDatasetsIfNeeded();
    reflectGroupChanges();
    return newGroup;
  }

  /**
   * Adds a group to the existing groups table
   */
  private void addGroup(ClientGroup group) {
    groups.put(group);

    logger.info("Add group " + group.getName() + " with " + group.getSamples().length + " samples " + "and "
        + group.getUnits().length + " units ");
  }

  @Override
  public void onResize() {
    splitPanel.onResize();
  }

  
  private class GroupColouring implements RowStyles<ClientGroup> {
    @Override
    public String getStyleNames(ClientGroup g, int rowIndex) {
      return g.getStyleName();
    }
  }
  
  // ExistingGroupsTable.Delegate methods
  @Override
  abstract public void makeGroupColumns(CellTable<ClientGroup> table);
  
  @Override
  public void displayGroupForEditing(String name) {
    setHeading("editing " + name);
    
    ClientGroup group = groups.storage().get(name);
    SampleClass sampleClass = 
        SampleClassUtils.asMacroClass(group.getSamples()[0].sampleClass(), schema);
    List<String> chosenCompounds = 
        SampleClassUtils.getMajors(schema, groups.storage().get(name), sampleClass).
        collect(Collectors.toList());
    setEditMode();
    
    currentlyEditingGroup = group;
    setupToolPanel();
    
    delegate.groupInspectorEditGroup(group, sampleClass, chosenCompounds);
  }
  
  @Override
  public void deleteGroup(String name) {
    groups.storage().remove(name);
    reflectGroupChanges();
    clearUiForNewGroup();
  }
  
  @Override
  public void existingGroupsTableSelectionChanged(Set<ClientGroup> selected) {
    groups.setActiveGroups(selected);
    groups.storage().saveToStorage();
    enableDatasetsIfNeeded();
    updateConfigureStatus(true);
  }
}
