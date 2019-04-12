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
    SelectionTDGrid.Delegate, ExistingGroupsTable.Delegate {

  public final Groups groups = new Groups();

  public SelectionTDGrid selectionGrid;
  private final OTGScreen screen;
  private final Delegate delegate;
  private final DataSchema schema;
  /**
   * Label above the selection grid 
   */
  private Label titleLabel;
  private TextBox groupNameTextBox;
  private Button saveButton, autoGroupsButton;
  private SelectionTable<Group> existingGroupsTable;
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
  /**
   * Whether the name entered in groupNameTextBox is an auto-generated name
   */
  private boolean nameIsAutoGen = false;

  protected final Logger logger = SharedUtils.getLogger("group");

  public interface Delegate {
    Future<SampleClass[]> enableDatasetsIfNeeded(Collection<Group> groups);
    void groupInspectorEditGroup(Group group, SampleClass sampleClass, List<String> compounds);
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
    verticalPanel.setWidth("440px");

    titleLabel = new Label("Sample group definition");
    titleLabel.addStyleName("heading");
    verticalPanel.add(titleLabel);

    selectionGrid = screen.factory().selectionTDGrid(screen, this);
    verticalPanel.add(selectionGrid);

    toolPanel = Utils.mkHorizontalPanel(true);
    verticalPanel.add(toolPanel);

    Label lblSaveGroupAs = new Label("Save group as");
    lblSaveGroupAs.addStyleName("slightlySpaced");
    toolPanel.add(lblSaveGroupAs);

    groupNameTextBox = new ImmediateValueChangeTextBox();
    groupNameTextBox.addValueChangeHandler(new ValueChangeHandler<String>() {
      @Override
      public void onValueChange(ValueChangeEvent<String> event) {
        nameIsAutoGen = false;
        onGroupNameInputChanged();
      }
    });
    groupNameTextBox.addKeyDownHandler(new KeyDownHandler() {
      // Pressing enter saves a group, but only if saving a new group rather than overwriting.
      @Override
      public void onKeyDown(KeyDownEvent event) {
        if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER && !groups.containsKey(groupNameTextBox.getValue())) {
          makeGroup(groupNameTextBox.getValue());
        }
      }
    });
    toolPanel.add(groupNameTextBox);

    saveButton = new Button("Save", (ClickHandler) e -> {
      makeGroup(groupNameTextBox.getValue());
    });
    toolPanel.add(saveButton);

    autoGroupsButton = new Button("Automatic groups", (ClickHandler) e -> {
      makeAutoGroups();      
    });
    toolPanel.add(autoGroupsButton);

    setEditMode();

    existingGroupsTable = new ExistingGroupsTable(this);
    existingGroupsTable.setVisible(false);
    existingGroupsTable.table().setRowStyles(new GroupColouring());
    existingGroupsTable.setSize("100%", "100px");
    splitPanel.addSouth(t.common.client.Utils.makeScrolled(existingGroupsTable), 200);

    splitPanel.add(t.common.client.Utils.makeScrolled(verticalPanel));
  }
  
  /**
   * Load groups from local storage and display them in the existing groups table.
   */
  public void loadGroups() {
    groups.loadGroups(screen.getStorage());
    enableDatasetsIfNeeded();
    updateConfigureStatus(false);

    // Reflect loaded group information in UI
    updateTableData();
    existingGroupsTable.table().redraw();
  }

  /**
   * Called when the value in the input box for entering a group's name changes, so
   * that the save button's text indicates whether a new group will be saved, or an
   * existing one will be overwritten. 
   */
  private void onGroupNameInputChanged() {
    if (groups.containsKey(groupNameTextBox.getValue())) {
      saveButton.setText("Overwrite");
    } else {
      saveButton.setText("Save");
    }
  }
  
  /**
   * Set the chosen datasets, sample class, and compounds for this group inspector's
   * selection grid. The datasetsChanged flag will determine whether the selection grid
   * will fetch time points when the sample class hasn't changed.
   */
  public void initializeState(List<Dataset> datasets, SampleClass sc, 
      List<String> compounds, boolean datasetsChanged) {
    selectionGrid.initializeState(sc, compounds, selectionGrid.getSelectedCombinations(), 
        datasetsChanged).addNonErrorCallback(() -> {
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
    selectionGrid.setCompounds(compounds).addNonErrorCallback(() -> {
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

  /**
   * Clears selections and input fields in the UI to prepare for the user entering
   * information for a new group.
   */
  private void clearUiForNewGroup() {
    groupNameTextBox.setText("");
    onGroupNameInputChanged();
    selectionGrid.setAll(false, true);
    delegate.groupInspectorClearCompounds();
    selectionGrid.setCompounds(new ArrayList<String>()).addNonErrorCallback(() -> {
      setEditMode();
    });
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
    groups.saveToLocalStorage(screen.getStorage());
    groupNameTextBox.setText("");
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
    List<Group> gs = GroupMaker.autoGroups(this, schema, selectionGrid.getAvailableUnits());
    for (Group g : gs) {
      addGroup(g, true);
    }
    // TODO investigate whether this enableDatasetsIfNeeded call can be removed
    enableDatasetsIfNeeded(); 
    reflectGroupChanges();
    clearUiForNewGroup();
  }

  /**
   * Create a group using the currently selected units in the selection grid, and 
   * save it using the provided name, assuming it's a valid group name. 
   * @param name
   */
  private void makeGroup(String name) {
    if (name.trim().equals("")) {
      Window.alert("Please enter a group name.");
      return;
    }
    if (!StorageProvider.isAcceptableString(name, "Unacceptable group name.")) {
      return;
    }

    List<Unit> units = selectionGrid.getSelectedUnits(false);

    if (units.size() == 0) {
      Window.alert("No samples found.");
    } else {
      Group newGroup = setGroup(name, units);
      clearUiForNewGroup();
      loadTimeWarningIfNeeded(newGroup);
    }
  }

  /**
   * Warn the user if the total number of unique samples in the active groups will be 
   * over 200 after the addition of newGroup.
   * @param newGroup the sample group the user is about to add
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
  
  //SelectionTDGrid.Delegate methods
  @Override
  public void selectedUnitsChanged(List<Unit> selectedUnits) {
    if (selectedUnits.isEmpty() && nameIsAutoGen) {
      //retract the previous suggestion
      groupNameTextBox.setText("");
    } else if (groupNameTextBox.getText().equals("") || nameIsAutoGen) {
      groupNameTextBox.setText(groups.suggestName(selectedUnits, schema));
      nameIsAutoGen = true;
    }
    onGroupNameInputChanged();
  }
  
  // ExistingGroupsTable.Delegate methods
  @Override
  abstract public void makeGroupColumns(CellTable<Group> table);
  
  @Override
  public void displayGroupForEditing(String name) {
    setHeading("editing " + name);
    groupNameTextBox.setValue(name);
    onGroupNameInputChanged();
    nameIsAutoGen = false;
    
    Group group = groups.get(name);
    SampleClass sampleClass = 
        SampleClassUtils.asMacroClass(group.getSamples()[0].sampleClass(), schema);
    List<String> chosenCompounds = 
        SampleClassUtils.getMajors(schema, groups.get(name), sampleClass).
        collect(Collectors.toList());
    setEditMode();
    
    delegate.groupInspectorEditGroup(group, sampleClass, chosenCompounds);
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
