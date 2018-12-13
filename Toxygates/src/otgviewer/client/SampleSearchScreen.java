/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
 * (NIBIOHN), Japan.
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

package otgviewer.client;

import java.util.*;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;

import otgviewer.client.components.*;
import t.common.shared.Dataset;
import t.common.shared.sample.*;
import t.model.SampleClass;
import t.model.sample.*;
import t.viewer.client.Utils;
import t.viewer.client.components.search.*;
import t.viewer.client.dialog.DialogPosition;
import t.viewer.client.rpc.SampleServiceAsync;

public class SampleSearchScreen extends MinimalScreen
    implements Search.Delegate, ResultTable.Delegate, FilterTools.Delegate {
  public static final String key = "search";

  private SampleServiceAsync sampleService;

  private FilterTools filterTools;

  protected SampleClass chosenSampleClass;
  protected List<Group> chosenColumns = new ArrayList<Group>();

  private Widget tools;
  private ConditionEditor conditionEditor;
  RadioButton individualSearchRadioButton;
  RadioButton unitSearchRadioButton;
  private MenuItem saveCVSMenuItem;
  private Button saveGroupButton;
  private Label resultCountLabel;

  private DialogBox waitDialog;

  private ResultTable<Sample> sampleTableHelper = new SampleTable(this);
  private ResultTable<Unit> unitTableHelper = new UnitTable(this);

  private SampleSearch sampleSearch;
  private UnitSearch unitSearch;
  private Search<?, ?> currentSearch = null;

  private Collection<Attribute> searchParameters;
  private Collection<Attribute> nonSearchParameters;

  private Collection<ParameterTickItem> parameterMenuItems;

  @Override
  public void loadState(AttributeSet attributes) {
    filterTools.datasetsChanged(getStorage().datasetsStorage.getIgnoringException());
    chosenSampleClass = getStorage().sampleClassStorage.getIgnoringException();
    filterTools.sampleClassChanged(chosenSampleClass);
    chosenColumns = getStorage().getChosenColumns();
  }

  private void getParameterInfo() {
    List<Attribute> searchParams = attributes().getNumerical();
    List<Attribute> nonSearchParams = attributes().getString();
    
    java.util.Collections.sort(searchParams, new AttributeComparator());
    java.util.Collections.sort(nonSearchParams, new AttributeComparator());

    searchParameters = searchParams;
    nonSearchParameters = nonSearchParams;
  }

  public SampleSearchScreen(ScreenManager man) {
    super("Sample search", key, man, man.resources().sampleSearchHTML(),
        man.resources().sampleSearchHelp());
    filterTools = new FilterTools(this);

    sampleService = man.sampleService();

    sampleSearch = new SampleSearch(this, sampleTableHelper, attributes(), sampleService);
    unitSearch = new UnitSearch(this, unitTableHelper, attributes(), sampleService);

    getParameterInfo();

    makeTools();
  }

  private void makeTools() {
    conditionEditor = new ConditionEditor(searchParameters);

    Button searchButton = new Button("Search", (ClickHandler) e -> {
        if (individualSearchRadioButton.getValue()) {
          sampleSearch.attemptSearch(chosenSampleClass, conditionEditor.getCondition());
        } else if (unitSearchRadioButton.getValue()) {
          unitSearch.attemptSearch(chosenSampleClass, conditionEditor.getCondition());
        }
      });

    individualSearchRadioButton = new RadioButton("searchTypeGroup", "Individual samples");
    unitSearchRadioButton = new RadioButton("searchTypeGroup", "Sample units");
    individualSearchRadioButton.setValue(true);
    VerticalPanel radioButtonPanel = Utils.mkVerticalPanel(true);
    radioButtonPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
    radioButtonPanel.add(individualSearchRadioButton);
    radioButtonPanel.add(unitSearchRadioButton);
    
    resultCountLabel = new Label();

    saveGroupButton = new Button("Save sample group", (ClickHandler) ev -> {
        try {
          if (currentSearch.helper().selectionTable().getSelection().size() > 0) {
            Unit[] allUnits = currentSearch.sampleGroupFromSelected();

            String name = findAvailableGroupName("Sample search group ");
            Group pendingGroup = new Group(schema(), name, allUnits);

            chosenColumns.add(pendingGroup);
          getStorage().chosenColumnsStorage.store(chosenColumns);

            currentSearch.helper().selectionTable().clearSelection();

            Window.alert("Saved group: " + name);
          } else {
            Window.alert("Sample groups must complain at least one unit. " + '\n'
                + "Please select some units before attempting to save a sample group.");
          }
        } catch (Exception e) {
          Window.alert("Saving group failed: " + e);
        }    
      });
    saveGroupButton.setEnabled(false);

    tools = Utils.mkVerticalPanel(true, conditionEditor, Utils.mkHorizontalPanel(true,
        searchButton, radioButtonPanel, resultCountLabel, saveGroupButton));
  }

  private String findAvailableGroupName(String prefix) throws Exception {
    List<Group> inactiveGroups =
        getStorage().getInactiveColumns();

    Set<String> groupNames = new HashSet<String>();
    for (Group group : chosenColumns) {
      groupNames.add(group.getName());
    }
    if (inactiveGroups != null) {
      for (Group group : inactiveGroups) {
        groupNames.add(group.getName());
      }
    }

    int n = 1;
    while (groupNames.contains(prefix + n)) {
      n++;
    }

    return prefix + n;
  }

  @Override
  protected Widget content() {
    setupMenuItems();

    ScrollPanel searchPanel = new ScrollPanel();
    unitTableHelper.selectionTable().setVisible(false);
    sampleTableHelper.selectionTable().setVisible(false);
    VerticalPanel vp =
        Utils.mkVerticalPanel(true, tools, unitTableHelper.selectionTable(),
            sampleTableHelper.selectionTable());
    searchPanel.add(vp);
    return searchPanel;
  }

  private class ParameterTickItem extends TickMenuItem {
    private Attribute attribute;

    public ParameterTickItem(MenuBar mb, Attribute attrib, boolean initState, boolean enabled) {
      super(mb, attrib.title(), initState);
      attribute = attrib;
      setEnabled(enabled);
    }

    public Attribute attribute() {
      return attribute;
    }

    @Override
    public void setState(boolean state) {
      super.setState(state);
    }

    @Override
    public void stateChange(boolean newState) {
      if (newState) {
        boolean waitForData = false;
        if (!currentSearch.hasParameter(attribute)) {
          currentSearch.fetchParameter(attribute);
          waitForData = true;
        }
        currentSearch.helper().addExtraColumn(attribute, attribute.isNumerical(), waitForData);
      } else {
        currentSearch.helper().removeAttributeColumn(attribute);
      }
    }
  }

  private void setupMenuItems() {
    MenuBar fileBar = new MenuBar(true);
    MenuItem fileItem = new MenuItem("File", false, fileBar);
    saveCVSMenuItem = new MenuItem("Save results to CSV", false, 
      () -> prepareCSVDownload());
      
    saveCVSMenuItem.setEnabled(false);
    fileBar.addItem(saveCVSMenuItem);
    addMenu(fileItem);

    MenuBar editBar = new MenuBar(true);
    MenuItem editItem = new MenuItem("Edit", false, editBar);
    MenuItem clearSearchConditionItem = new MenuItem("Clear search condition", false, 
          () -> conditionEditor.clear());
    
    editBar.addItem(clearSearchConditionItem);
    MenuItem clearSelectionItem = new MenuItem("Clear selection", false, 
      () -> currentSearch.helper().selectionTable().clearSelection());
      
    editBar.addItem(clearSelectionItem);
    addMenu(editItem);

    parameterMenuItems = new ArrayList<ParameterTickItem>();
    MenuBar parametersBar = new MenuBar(true);
    MenuItem parameterItem = new MenuItem("View", false, parametersBar);
    MenuBar numericalParametersBar = new MenuBar(true);
    MenuItem numericalParametersItem =
        new MenuItem("Numerical parameters", false, numericalParametersBar);
    for (Attribute attribute : searchParameters) {
      parameterMenuItems.add(new ParameterTickItem(numericalParametersBar,
          attribute, false, false));
    }
    MenuBar stringParametersBar = new MenuBar(true);
    MenuItem stringParametersItem =
        new MenuItem("Non-numerical parameters", false, stringParametersBar);
    for (Attribute attribute : nonSearchParameters) {
      parameterMenuItems.add(new ParameterTickItem(stringParametersBar,
          attribute, false, false));
    }
    parametersBar.addItem(numericalParametersItem);
    parametersBar.addItem(stringParametersItem);
    addMenu(parameterItem);
  }

  @Override
  protected void addToolbars() {
    super.addToolbars();
    HorizontalPanel hp = Utils.mkHorizontalPanel(false, filterTools);
    addToolbar(hp, 0);
  }

  @Override
  public String getGuideText() {
    return "Here you can search for samples and units based on values of biological parameters.";
  }

  @Override
  protected boolean shouldShowStatusBar() {
    return false;
  }

  protected void hideTables() {
    sampleTableHelper.clear();
    sampleTableHelper.selectionTable().setVisible(false);
    unitTableHelper.clear();
    unitTableHelper.selectionTable().setVisible(false);
  }

  private void prepareCSVDownload() {
    this.sampleService.prepareCSVDownload((SampleLike[]) currentSearch.searchResult(),
        currentSearch.helper().allAttributes(), new PendingAsyncCallback<String>(
            SampleSearchScreen.this,
            "Unable to prepare the data for download,") {
          @Override
          public void handleSuccess(String url) {
            Utils.displayURL("Your download is ready.", "Download", url);
          }
        });
  }

  /*
   * Search.Delegate methods
   */
  @Override
  public void searchStarted(Search<?, ?> search) {
    if (waitDialog == null) {
      waitDialog = Utils.waitDialog();
    } else {
      waitDialog.show();
    }
  }

  @Override
  public void searchEnded(Search<?, ?> search, String resultCountText) {
    waitDialog.hide();
    hideTables();
    resultCountLabel.setText(resultCountText);
    currentSearch = search;
    saveCVSMenuItem.setEnabled(true);
    saveGroupButton.setEnabled(true);
  }

  /*
   * ResultTable.Delegate methods
   */
  @Override
  public ImageResource inspectCellImage() {
    return manager.resources().magnify();
  }

  @Override
  public void finishedSettingUpTable() {
    currentSearch.helper().selectionTable().setVisible(true);
    currentSearch.helper().selectionTable().clearSelection();

    HashSet<Attribute> requiredAttributes =
        new HashSet<Attribute>(Arrays.asList(currentSearch.helper().requiredAttributes()));
    HashSet<Attribute> nonRequiredAttributes =
        new HashSet<Attribute>(Arrays.asList(currentSearch.helper().nonRequiredAttributes()));

    for (ParameterTickItem item : parameterMenuItems) {
      if (requiredAttributes.contains(item.attribute())) {
        item.setState(true);
        item.setEnabled(false);
      } else if (nonRequiredAttributes.contains(item.attribute())) {
        item.setState(true);
        item.setEnabled(true);
      } else {
        item.setState(false);
        item.setEnabled(true);
      }
    }
  }

  @Override
  public void displayDetailsForEntry(Unit unit) {
    SampleDetailTable table = new SampleDetailTable(this, "Unit details", false);
    
    List<Unit> singleUnitList = new ArrayList<Unit>();
    singleUnitList.add(unit);
    Unit[] unitsWithControl = unitSearch.sampleGroupFromEntities(singleUnitList);
    Group g = new Group(schema(), "data", unitsWithControl);

    table.loadFrom(g, false);
    Utils.displayInPopup("Unit details", table, DialogPosition.Top);
  }

  @Override
  protected TextResource getHelpHTML() {
    return super.getHelpHTML();
  }

  @Override
  protected ImageResource getHelpImage() {
    return resources().sampleSearchHelp();
  }

  //FilterTools.Delegate method
  @Override
  public void filterToolsSampleClassChanged(SampleClass sc) {
    chosenSampleClass = sc;
    getStorage().sampleClassStorage.store(sc);
  }

  @Override
  public void filterToolsDatasetsChanged(List<Dataset> datasets) {
    getStorage().datasetsStorage.store(datasets);
  }
}
