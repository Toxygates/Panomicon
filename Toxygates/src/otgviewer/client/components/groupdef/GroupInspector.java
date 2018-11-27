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

package otgviewer.client.components.groupdef;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.google.gwt.cell.client.*;
import com.google.gwt.cell.client.ButtonCellBase.DefaultAppearance.Style;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.cellview.client.*;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;

import otg.model.sample.OTGAttribute;
import otgviewer.client.components.*;
import otgviewer.client.components.compoundsel.CompoundSelector;
import t.common.client.components.SelectionTable;
import t.common.shared.*;
import t.common.shared.sample.*;
import t.model.SampleClass;
import t.model.sample.CoreParameter;
import t.viewer.client.*;
import t.viewer.client.components.ImmediateValueChangeTextBox;
import t.viewer.client.rpc.SampleServiceAsync;

/**
 * This widget is intended to help visually define and modify groups of samples. The main dose/time
 * grid is implemented in the SelectionTDGrid. The rest is in this class.
 */
abstract public class GroupInspector extends Composite implements RequiresResize,
    SelectionTDGrid.UnitListener {

  public final Groups groups = new Groups();

  private MultiSelectionGrid multiSelectionGrid;
  private final Screen screen;
  private final Delegate delegate;
  private final DataSchema schema;
  private Label titleLabel;
  private TextBox txtbxGroup;
  private Button saveButton, autoGroupsButton;
  private SelectionTable<Group> existingGroupsTable;
  private CompoundSelector compoundSel;
  private HorizontalPanel toolPanel;
  private SplitLayoutPanel splitPanel;
  private VerticalPanel verticalPanel;
  private boolean nameIsAutoGen = false;

  private List<Pair<Unit, Unit>> availableUnits;

  protected final Logger logger = SharedUtils.getLogger("group");
  private final SampleServiceAsync sampleService;

  protected Dataset[] chosenDatasets = new Dataset[0];
  protected SampleClass chosenSampleClass;
  protected List<String> chosenCompounds = new ArrayList<String>();

  public interface Delegate {
    void groupInspectorDatasetsChanged(Dataset[] ds);
    void groupInspectorSampleClassChanged(SampleClass sc);
  }

  public interface ButtonCellResources extends ButtonCellBase.DefaultAppearance.Resources {
    @Override
    @Source("otgviewer/client/ButtonCellBase.gss")
    Style buttonCellBaseStyle();
  }

  public GroupInspector(CompoundSelector cs, Screen scr, Delegate delegate) {
    compoundSel = cs;
    this.screen = scr;
    this.delegate = delegate;
    schema = scr.schema();
    sampleService = scr.manager().sampleService();
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

    setEditing(false);

    existingGroupsTable = new SelectionTable<Group>("Active", false) {
      @Override
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
            return object.getSamples()[0].get(CoreParameter.Type);
          }
        };
        table.addColumn(textColumn, "Type");
        
        makeGroupColumns(table);

        ButtonCellResources resources = GWT.create(ButtonCellResources.class);
        TextButtonCell.Appearance appearance = new TextButtonCell.DefaultAppearance(resources);

        // We use TextButtonCell instead of ButtonCell since it has setEnabled
        final TextButtonCell editCell = new TextButtonCell(appearance);

        Column<Group, String> editColumn = new Column<Group, String>(editCell) {
          @Override
          public String getValue(Group g) {
            editCell.setEnabled(!groups.isStatic(g));
            return "Edit";
          }
        };
        editColumn.setFieldUpdater(new FieldUpdater<Group, String>() {
          @Override
          public void update(int index, Group object, String value) {
            displayGroup(object.getName());
          }
        });
        table.addColumn(editColumn, "");

        final TextButtonCell deleteCell = new TextButtonCell(appearance);
        Column<Group, String> deleteColumn = new Column<Group, String>(deleteCell) {
          @Override
          public String getValue(Group g) {
            deleteCell.setEnabled(!groups.isStatic(g));
            return "Delete";
          }
        };
        deleteColumn.setFieldUpdater(new FieldUpdater<Group, String>() {
          @Override
          public void update(int index, Group object, String value) {
            if (Window.confirm("Are you sure you want to delete the group " + object.getName()
                + "?")) {
              deleteGroup(object.getName(), true);
            }
          }

        });
        table.addColumn(deleteColumn, "");

      }

      @Override
      protected void selectionChanged(Set<Group> selected) {
        groups.chosenColumns = new ArrayList<Group>(selected);
        storeColumns();
        updateConfigureStatus(true);
      }
    };
    // vp.add(existingGroupsTable);
    existingGroupsTable.setVisible(false);
    existingGroupsTable.table().setRowStyles(new GroupColouring());
    existingGroupsTable.setSize("100%", "100px");
    splitPanel.addSouth(t.common.client.Utils.makeScrolled(existingGroupsTable), 200);

    splitPanel.add(t.common.client.Utils.makeScrolled(verticalPanel));
  }

  private void onGroupNameInputChanged() {
    if (groups.containsKey(txtbxGroup.getValue())) {
      saveButton.setText("Overwrite");
    } else {
      saveButton.setText("Save");
    }
  }

  abstract protected void makeGroupColumns(CellTable<Group> table);

  public SelectionTable<Group> existingGroupsTable() {
    return existingGroupsTable;
  }

  public void addStaticGroups(Group[] staticGroups) {
    for (Group g : staticGroups) {
      addGroup(g, false);
      groups.staticGroupNames.add(g.getName());
    }
    reflectGroupChanges(false);
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

  private void deleteGroup(String name, boolean createNew) {
    groups.remove(name);
    reflectGroupChanges(true); // stores columns
    if (createNew) {
      prepareForNewGroup();
    }
  }

  public void confirmDeleteAllGroups() {
    int numberToDelete = existingGroupsTable.getItems().size() - groups.staticGroupNames.size();
    if (Window.confirm("Delete " + numberToDelete + " groups?")) {
      groups.clearNonStatic();
      reflectGroupChanges(true);
      prepareForNewGroup();
    }
  }

  /**
   * Toggle edit mode
   */
  private void setEditing(boolean editing) {
    boolean val = editing && (chosenCompounds.size() > 0);
    toolPanel.setVisible(val);
  }

  private void setHeading(String title) {
    titleLabel.setText("Sample group definition - " + title);
  }

  private void prepareForNewGroup() {
    txtbxGroup.setText("");
    onGroupNameInputChanged();
    multiSelectionGrid.setAll(false);
    compoundSel.setSelection(new ArrayList<String>());
    setHeading("new group");
    setEditing(true);
  }

  private List<Group> sortedGroupList(Collection<Group> groups) {
    ArrayList<Group> r = new ArrayList<Group>(groups);
    Collections.sort(r);
    return r;
  }

  /**
   * To be called when groups are added or deleted.
   * 
   * @param store if the new group list should be stored or not
   */
  private void reflectGroupChanges(boolean store) {
    existingGroupsTable.setItems(sortedGroupList(groups.values()), false);
    groups.chosenColumns = new ArrayList<Group>(existingGroupsTable.getSelection());
    logger.info(groups.chosenColumns.size() + " columns have been chosen");
    if (store) {
      storeColumns();
    }
    txtbxGroup.setText("");
    updateConfigureStatus(true);
    existingGroupsTable.setVisible(groups.size() > 0);

  }

  private void updateConfigureStatus(boolean internalTriggered) {
    enableDatasetsIfNeeded(groups.chosenColumns);
    if (internalTriggered) {
      screen.manager().resetWorkflowLinks();
    }
  }

  public void sampleClassChanged(SampleClass sc) {
    chosenSampleClass = sc;
    if (!sc.equals(chosenSampleClass)) {
      compoundsChanged(new ArrayList<String>());
    }
    multiSelectionGrid.sampleClassChanged(sc);
  }

  public void datasetsChanged(Dataset[] ds) {
    chosenDatasets = ds;
    disableGroupsIfNeeded(ds);
  }

  protected void disableGroupsIfNeeded(Dataset[] datasets) {
    Set<String> availableDatasets = new HashSet<String>();
    for (Dataset d : datasets) {
      availableDatasets.add(d.getId());
    }
    int disableCount = 0;

    logger.info("Available DS: " + SharedUtils.mkString(availableDatasets, ", "));
    for (Group group : existingGroupsTable.getSelection()) {
      List<String> requiredDatasets = group.collect(OTGAttribute.Dataset).collect(Collectors.toList());
      logger.info("Group " + group.getShortTitle() + " needs " + SharedUtils.mkString(requiredDatasets, ", "));
      if (!availableDatasets.containsAll(requiredDatasets)) {
        existingGroupsTable.unselect(group);
        disableCount += 1;
      }
    }
    if (disableCount > 0) {
      reflectGroupChanges(true);
      Window
          .alert(disableCount + " group(s) were deactivated " + "because of your dataset choice.");
    }
  }

  protected void enableDatasetsIfNeeded(Collection<Group> gs) {
    List<String> neededDatasets = Group.collectAll(gs, OTGAttribute.Dataset).collect(Collectors.toList());
    logger.info("Needed datasets: " + SharedUtils.mkString(neededDatasets, ", "));

    Dataset[] allDatasets = screen.appInfo().datasets();
    Set<String> enabled = new HashSet<String>();
    for (Dataset d : chosenDatasets) {
      enabled.add(d.getId());
    }
    logger.info("Enabled: " + SharedUtils.mkString(enabled, ", "));
    if (!enabled.containsAll(neededDatasets)) {
      HashSet<String> missing = new HashSet<String>(neededDatasets);
      missing.removeAll(enabled);

      List<Dataset> newEnabledList = new ArrayList<Dataset>();
      for (Dataset d : allDatasets) {
        if (enabled.contains(d.getId()) || neededDatasets.contains(d.getId())) {
          newEnabledList.add(d);
        }
      }
      Dataset[] newEnabled = newEnabledList.toArray(new Dataset[0]);
      datasetsChanged(newEnabled);
      delegate.groupInspectorDatasetsChanged(newEnabled);
      sampleService.chooseDatasets(newEnabled, new PendingAsyncCallback<SampleClass[]>(screen));
      screen.getParser().storeDatasets(chosenDatasets);
      Window
          .alert(missing.size() + " dataset(s) were activated " + "because of your group choice.");
    }
  }

  public void loadGroups() {
    groups.loadGroups(screen.getParser(), screen.schema(), screen.attributes());
    updateConfigureStatus(false);

    // Reflect loaded group information in UI
    existingGroupsTable.setItems(groups.all(), false);
    existingGroupsTable.setSelection(groups.chosen());
    existingGroupsTable.table().redraw();
    existingGroupsTable.setVisible(groups.size() > 0);
    prepareForNewGroup();
  }

  public void compoundsChanged(List<String> compounds) {
    chosenCompounds = compounds;
    if (compounds.size() == 0) {
      setEditing(false);
    } else {
      setEditing(true);
    }
    multiSelectionGrid.compoundsChanged(compounds);
  }

  private void storeColumns() {
    screen.getParser().storeColumns("columns", groups.chosenColumns);
    screen.getParser().storeColumns("inactiveColumns",
        new ArrayList<SampleColumn>(existingGroupsTable.inverseSelection()));
  }

  private void makeAutoGroups() {
    List<Group> gs = GroupMaker.autoGroups(this, schema, availableUnits);
    for (Group g : gs) {
      addGroup(g, true);
    }
    reflectGroupChanges(true);
  }

  /**
   * Get here if save button is clicked
   */
  private void makeGroup(String name) {
    if (name.trim().equals("")) {
      Window.alert("Please enter a group name.");
      return;
    }
    if (!StorageParser.isAcceptableString(name, "Unacceptable group name.")) {
      return;
    }

    List<Unit> units = multiSelectionGrid.fullSelection(false);

    if (units.size() == 0) {
      Window.alert("No samples found.");
    } else {
      Group newGroup = setGroup(name, units);
      prepareForNewGroup();
      loadTimeWarningIfNeeded(newGroup);
    }
  }

  private void loadTimeWarningIfNeeded(Group newGroup) {
    int newGroupSize = 0;
    int totalSize = 0;

    HashSet<String> sampleIds = new HashSet<String>();

    for (Sample sample : newGroup.samples()) {
      if (!schema.isSelectionControl(sample.sampleClass())) {
        sampleIds.add(sample.id());
        newGroupSize += 1;
        totalSize += 1;
      }
    }

    for (Group group : existingGroupsTable.getSelection()) {
      for (Sample sample : group.samples()) {
        if (!schema.isSelectionControl(sample.sampleClass())
            && !sampleIds.contains(sample.id())) {
          sampleIds.add(sample.id());
          totalSize += 1;
        }
      }
    }

    // Conservatively estimate that we load 10 samples per second
    int loadTime = totalSize / 10;

    if (loadTime > 20) {
      Window.alert("Warning: Your new group contains " + newGroupSize + " samples.\n"
          + "You will now be requesting data for " + totalSize + " samples.\n"
          + "The total loading time is expected to be " + loadTime + " seconds.");
    }
  }

  private Group setGroup(String pendingGroupName, List<Unit> units) {
    logger.info("Set group with " + SharedUtils.mkString(units, ","));
    Group pendingGroup = groups.get(pendingGroupName);
    if (pendingGroup == null) {
      Analytics.trackEvent(Analytics.CATEGORY_GENERAL, Analytics.ACTION_CREATE_NEW_SAMPLE_GROUP);
    } else {
      Analytics.trackEvent(Analytics.CATEGORY_GENERAL,
          Analytics.ACTION_MODIFY_EXISTING_SAMPLE_GROUP);
    }
    existingGroupsTable.removeItem(pendingGroup);
    pendingGroup = new Group(schema, pendingGroupName, units.toArray(new Unit[0]));
    addGroup(pendingGroup, true);
    reflectGroupChanges(true);
    return pendingGroup;
  }

  private void addGroup(Group group, boolean active) {
    String name = group.getName();
    groups.put(name, group);
    logger.info("Add group " + name + " with " + group.getSamples().length + " samples " + "and "
        + group.getUnits().length + " units ");

    existingGroupsTable.addItem(group);
    if (active) {
      existingGroupsTable.select(group);
    }
  }

  private void displayGroup(String name) {
    setHeading("editing " + name);
    Group g = groups.get(name);
    SampleClass macroClass = 
        SampleClassUtils.asMacroClass(g.getSamples()[0].sampleClass(), schema);
    sampleClassChanged(macroClass);
    delegate.groupInspectorSampleClassChanged(macroClass);

    List<String> compounds = 
        SampleClassUtils.getMajors(schema, groups.get(name), chosenSampleClass).
        collect(Collectors.toList());

    compoundSel.setSelection(compounds);
    txtbxGroup.setValue(name);
    onGroupNameInputChanged();
    nameIsAutoGen = false;

    multiSelectionGrid.setSelection(g.getUnits());

    setEditing(true);
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
}
