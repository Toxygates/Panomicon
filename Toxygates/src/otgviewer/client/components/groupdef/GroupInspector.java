/*
 * Copyright (c) 2012-2017 Toxygates authors, National Institutes of Biomedical Innovation, Health
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

import static t.common.client.Utils.makeScrolled;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import otg.model.sample.OTGAttribute;
import otgviewer.client.components.*;
import otgviewer.client.components.compoundsel.CompoundSelector;
import t.common.client.components.SelectionTable;
import t.common.shared.*;
import t.common.shared.sample.*;
import t.model.SampleClass;
import t.model.sample.CoreParameter;
import t.viewer.client.*;
import t.viewer.client.rpc.SampleServiceAsync;

import com.google.gwt.cell.client.*;
import com.google.gwt.cell.client.ButtonCellBase.DefaultAppearance.Style;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.cellview.client.*;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;

/**
 * This widget is intended to help visually define and modify groups of samples. The main dose/time
 * grid is implemented in the SelectionTDGrid. The rest is in this class.
 */
abstract public class GroupInspector extends DataListenerWidget implements RequiresResize,
    SelectionTDGrid.UnitListener {

  private MultiSelectionGrid msg;
  private Map<String, Group> groups = new HashMap<String, Group>();
  private final DLWScreen screen;
  private final DataSchema schema;
  private Label titleLabel;
  private TextBox txtbxGroup;
  private Button saveButton, autoGroupsButton;
  private SelectionTable<Group> existingGroupsTable;
  private CompoundSelector compoundSel;
  private HorizontalPanel toolPanel;
  private SplitLayoutPanel sp;
  private VerticalPanel vp;
  private boolean nameIsAutoGen = false;
  private Set<String> staticGroupNames = new HashSet<String>();

  private List<Pair<Unit, Unit>> availableUnits;

  protected final Logger logger = SharedUtils.getLogger("group");
  private final SampleServiceAsync sampleService;

  public interface ButtonCellResources extends ButtonCellBase.DefaultAppearance.Resources {
    @Override
    @Source("otgviewer/client/ButtonCellBase.gss")
    Style buttonCellBaseStyle();
  }

  public GroupInspector(CompoundSelector cs, DLWScreen scr) {
    compoundSel = cs;
    this.screen = scr;
    schema = scr.schema();
    sampleService = scr.manager().sampleService();
    sp = new SplitLayoutPanel();
    initWidget(sp);

    vp = Utils.mkTallPanel();

    titleLabel = new Label("Sample group definition");
    titleLabel.addStyleName("heading");
    vp.add(titleLabel);

    msg = new MultiSelectionGrid(scr, this);
    vp.add(msg);
    addListener(msg);

    vp.setWidth("440px");

    toolPanel = Utils.mkHorizontalPanel(true);
    vp.add(toolPanel);

    Label lblSaveGroupAs = new Label("Save group as");
    lblSaveGroupAs.addStyleName("slightlySpaced");
    toolPanel.add(lblSaveGroupAs);

    txtbxGroup = new TextBox();
    txtbxGroup.addValueChangeHandler(new ValueChangeHandler<String>() {
      @Override
      public void onValueChange(ValueChangeEvent<String> event) {
        nameIsAutoGen = false;
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
            editCell.setEnabled(!isStatic(g));
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
            deleteCell.setEnabled(!isStatic(g));
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
        chosenColumns = new ArrayList<Group>(selected);
        StorageParser p = getParser(screen);
        storeColumns(p);
        updateConfigureStatus(true);
      }
    };
    // vp.add(existingGroupsTable);
    existingGroupsTable.setVisible(false);
    existingGroupsTable.table().setRowStyles(new GroupColouring());
    existingGroupsTable.setSize("100%", "100px");
    sp.addSouth(makeScrolled(existingGroupsTable), 200);

    sp.add(makeScrolled(vp));
  }

  abstract protected void makeGroupColumns(CellTable<Group> table);

  public SelectionTable<Group> existingGroupsTable() {
    return existingGroupsTable;
  }

  public void addStaticGroups(Group[] staticGroups) {
    for (Group g : staticGroups) {
      addGroup(g, false);
      staticGroupNames.add(g.getName());
    }
    reflectGroupChanges(false);
  }

  private boolean isStatic(Group g) {
    return staticGroupNames.contains(g.getName());
  }

  private void clearNonStaticGroups() {
    Set<String> keys = new HashSet<String>(groups.keySet());
    for (String k : keys) {
      if (!isStatic(groups.get(k))) {
        groups.remove(k);
      }
    }
  }

  /**
   * Callback from SelectionTDGrid
   * 
   * @param selectedUnits
   */
  @Override
  public void unitsChanged(DataListenerWidget sender, List<Unit> selectedUnits) {
    if (selectedUnits.isEmpty() && nameIsAutoGen) {
      //retract the previous suggestion
      txtbxGroup.setText("");
    } else if (txtbxGroup.getText().equals("") || nameIsAutoGen) {
      txtbxGroup.setText(suggestGroupName(selectedUnits));
      nameIsAutoGen = true;
    } 
  }

  @Override
  public void availableUnitsChanged(DataListenerWidget sender, List<Pair<Unit, Unit>> units) {
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
    int n = existingGroupsTable.getItems().size();
    int fn = n - staticGroupNames.size();
    if (Window.confirm("Delete " + fn + " groups?")) {
      clearNonStaticGroups();
      reflectGroupChanges(true);
      prepareForNewGroup();
    }
  }

  /**
   * Toggle edit mode
   * 
   * @param editing
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

  /**
   * To be called when groups are added or deleted.
   * 
   * @param store if the new group list should be stored or not
   */
  private void reflectGroupChanges(boolean store) {
    existingGroupsTable.setItems(sortedGroupList(groups.values()), false);
    chosenColumns = new ArrayList<Group>(existingGroupsTable.getSelection());
    logger.info(chosenColumns.size() + " columns have been chosen");
    StorageParser p = getParser(screen);
    if (store) {
      storeColumns(p);
    }
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
    enableDatasetsIfNeeded(chosenColumns);
    if (internalTriggered) {
      screen.manager().reconfigureAll(screen.key());
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
      g =
          firstChars(b.get(schema.majorParameter())) + "/"
              + b.get(schema.mediumParameter()).substring(0, 1) + "/"
              + b.get(schema.minorParameter());
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
      super.sampleClassChanged(sc);
      // groups.clear();
      // existingGroupsTable.setItems(new ArrayList<Group>(), true);
      compoundsChanged(new ArrayList<String>());
      // newGroup();
    } else {
      super.sampleClassChanged(sc);
    }
  }

  @Override
  public void datasetsChanged(Dataset[] ds) {
    super.datasetsChanged(ds);
    disableGroupsIfNeeded(ds);
  }

  protected void disableGroupsIfNeeded(Dataset[] ds) {
    Set<String> availDs = new HashSet<String>();
    for (Dataset d : ds) {
      availDs.add(d.getTitle());
    }
    int disableCount = 0;

    logger.info("Available DS: " + SharedUtils.mkString(availDs, ", "));
    for (Group g : existingGroupsTable.getSelection()) {
      List<String> reqDs = g.collect(OTGAttribute.Dataset).collect(Collectors.toList());
      logger.info("Group " + g.getShortTitle() + " needs " + SharedUtils.mkString(reqDs, ", "));
      if (!availDs.containsAll(reqDs)) {
        existingGroupsTable.unselect(g);
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
      enabled.add(d.getTitle());
    }
    logger.info("Enabled: " + SharedUtils.mkString(enabled, ", "));
    if (!enabled.containsAll(neededDatasets)) {
      HashSet<String> missing = new HashSet<String>(neededDatasets);
      missing.removeAll(enabled);

      List<Dataset> newEnabled = new ArrayList<Dataset>();
      for (Dataset d : allDatasets) {
        if (enabled.contains(d.getTitle()) || neededDatasets.contains(d.getTitle())) {
          newEnabled.add(d);
        }
      }
      Dataset[] enAr = newEnabled.toArray(new Dataset[0]);
      screen.datasetsChanged(enAr);
      sampleService.chooseDatasets(enAr, new PendingAsyncCallback<SampleClass[]>(screen));
      storeDatasets(getParser(screen));
      Window
          .alert(missing.size() + " dataset(s) were activated " + "because of your group choice.");
    }
  }

  @Override
  public void columnsChanged(List<Group> columns) {
    super.columnsChanged(columns);
    clearNonStaticGroups();

    for (Group g : columns) {
      groups.put(g.getName(), g);
    }
    updateConfigureStatus(false);

    existingGroupsTable.setItems(sortedGroupList(groups.values()), true);
    existingGroupsTable.setSelection(chosenColumns);
    existingGroupsTable.setVisible(groups.size() > 0);
    prepareForNewGroup();
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
    all.addAll(sortedGroupList(existingGroupsTable.getSelection()));
    all.addAll(igs);
    existingGroupsTable.setItems(all, false);
    existingGroupsTable.unselectAll(igs);
    existingGroupsTable.table().redraw();
    existingGroupsTable.setVisible(groups.size() > 0);
    prepareForNewGroup();
  }

  @Override
  public void storeColumns(StorageParser p) {
    super.storeColumns(p);
    storeColumns(p, "inactiveColumns",
        new ArrayList<SampleColumn>(existingGroupsTable.inverseSelection()));
  }

  public Map<String, Group> getGroups() {
    return groups;
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
   * 
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
    changeSampleClass(macroClass);
    screen.sampleClassChanged(macroClass);

    List<String> compounds = 
        SampleClassUtils.getMajors(schema, groups.get(name), chosenSampleClass).
        collect(Collectors.toList());

    compoundSel.setSelection(compounds);
    txtbxGroup.setValue(name);
    nameIsAutoGen = false;

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
