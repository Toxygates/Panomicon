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

package t.viewer.client.screen;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import t.viewer.client.ClientGroup;
import t.viewer.client.Groups;
import t.viewer.client.Utils;
import t.viewer.client.components.*;
import t.shared.common.Dataset;
import t.shared.common.sample.Group;
import t.shared.common.sample.Sample;
import t.shared.common.sample.Unit;
import t.model.SampleClass;
import t.model.sample.Attribute;
import t.model.sample.AttributeSet;
import t.model.sample.SampleLike;
import t.viewer.client.components.search.*;
import t.viewer.client.dialog.DialogPosition;
import t.viewer.client.future.Future;
import t.viewer.client.rpc.SampleServiceAsync;

import java.util.*;

public class SampleSearchScreen extends FilterScreen
    implements Search.Delegate, ResultTable.Delegate, FilterTools.Delegate {
  public static final String key = "search";

  private SampleServiceAsync sampleService;

  protected List<Dataset> chosenDatasets;
  protected Groups groups;

  private Widget tools;
  private ConditionEditor conditionEditor;
  RadioButton individualSearchRadioButton;
  RadioButton unitSearchRadioButton;
  private MenuItem saveCVSMenuItem;
  private Button saveGroupButton;
  private Label resultCountLabel;

  private ResultTable<Sample> sampleTableHelper = new SampleTable(this);
  private ResultTable<Unit> unitTableHelper = new UnitTable(this);

  private SampleSearch sampleSearch;
  private UnitSearch unitSearch;
  private Search<?, ?> currentSearch = null;

  private Collection<Attribute> numericalParameters;
  private Collection<Attribute> stringParameters;
  
  private MenuItem numericalParametersItem;
  private MenuItem stringParametersItem;
  private MenuBar numericalParametersBar;
  private MenuBar stringParametersBar;

  private List<ParameterTickItem> numericalParameterMenuItems;
  private List<ParameterTickItem> stringParameterMenuItems;
  
  Set<Attribute> enabledAttributesForCurrentSampleFilter = new HashSet<Attribute>();

  @Override
  public void loadState(AttributeSet attributes) {
    List<Dataset> newChosenDatasets = getStorage().datasetsStorage.getIgnoringException();
    filterTools.setDatasets(newChosenDatasets);
    if (!newChosenDatasets.equals(chosenDatasets)) {
      fetchSampleClasses(new Future<SampleClass[]>(), newChosenDatasets).addCallback(f -> {
        filterTools.setSampleClass(getStorage().sampleClassStorage.getIgnoringException());
        fetchAttributesForSampleClass(filterTools.dataFilterEditor.currentSampleClassShowing());
      });
      chosenDatasets = newChosenDatasets;
    }
    groups.storage().loadFromStorage();
  }

  private void getParameterInfo() {
    List<Attribute> searchParams = attributes().getNumerical();
    List<Attribute> nonSearchParams = attributes().getString();
    
    java.util.Collections.sort(searchParams);
    java.util.Collections.sort(nonSearchParams);

    numericalParameters = searchParams;
    stringParameters = nonSearchParams;
  }

  public SampleSearchScreen(ScreenManager man) {
    super("Sample search", key, man, man.resources().sampleSearchHTML(),
        man.resources().sampleSearchHelp());
    
    groups = new Groups(getStorage().groupsStorage);
    filterTools = new FilterTools(this);

    sampleService = man.sampleService();

    sampleSearch = new SampleSearch(this, sampleTableHelper, sampleService);
    unitSearch = new UnitSearch(this, unitTableHelper, sampleService);

    getParameterInfo();

    makeTools();
  }

  private void makeTools() {
    conditionEditor = new ConditionEditor(numericalParameters);

    Button searchButton = new Button("Search", (ClickHandler) e -> {
      SampleClass chosenSampleClass = filterTools.dataFilterEditor.currentSampleClassShowing();
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

            String name = groups.storage().suggestName("Sample search group");
            ClientGroup pendingGroup = new ClientGroup(schema(), name, allUnits, true,
                groups.nextColor());

            groups.put(pendingGroup);
            groups.storage().saveToStorage();

            currentSearch.helper().selectionTable().clearSelection();

            manager().resetWorkflowLinks();

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
    
    public ParameterTickItem(Attribute attrib, boolean initState, boolean enabled) {
      super(attrib.title(), initState, true);
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

    // We construct the menu items here, but do not add them 
    numericalParameterMenuItems = new ArrayList<ParameterTickItem>(numericalParameters.size());
    for (Attribute attribute : numericalParameters) {
      numericalParameterMenuItems.add(new ParameterTickItem(attribute, false, false));
    }
    stringParameterMenuItems = new ArrayList<ParameterTickItem>(stringParameters.size());
    for (Attribute attribute : stringParameters) {
      stringParameterMenuItems.add(new ParameterTickItem(attribute, false, false));
    }
    
    MenuBar parametersBar = new MenuBar(true);
    MenuItem parameterItem = new MenuItem("View", false, parametersBar);
    
    numericalParametersBar = new MenuBar(true);
    numericalParametersItem =
        new MenuItem("Numerical parameters", false, numericalParametersBar);
    parametersBar.addItem(numericalParametersItem);
    
    stringParametersBar = new MenuBar(true);
    stringParametersItem =
        new MenuItem("Non-numerical parameters", false, stringParametersBar);
    parametersBar.addItem(stringParametersItem);
    
    updateMenuItemExistence();
    
    addMenu(parameterItem);
  }

  @Override
  protected void addToolbars() {
    super.addToolbars();
    HorizontalPanel hp = Utils.mkHorizontalPanel(false, filterTools);
    addToolbar(hp, 45);
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
          SampleSearchScreen.this.manager, "Unable to prepare the data for download.",
          (String url) -> {
            Utils.displayURL("Your download is ready.", "Download", url);
          }));
  }
  
  /**
   * Updates the enabled attribute set. Called (eventually) on load, and after sampleclass
   * changes. 
   */
  private void updateEnabledAttributeSet(Attribute[] newEnabledSet) {
    enabledAttributesForCurrentSampleFilter = 
        new HashSet<Attribute>(Arrays.asList(newEnabledSet));
    
    Set<Attribute> enabledNumericalParameters = new HashSet<Attribute>(numericalParameters);
    enabledNumericalParameters.retainAll(Arrays.asList(newEnabledSet));
    conditionEditor.updateParameters(enabledNumericalParameters);
  }
  
  /**
   * Called on load, 
   */
  private void updateMenuItemExistence() {
    updateMenuItemExistenceForOneMenu(numericalParametersBar, numericalParameterMenuItems, 
        numericalParametersItem);
    updateMenuItemExistenceForOneMenu(stringParametersBar, stringParameterMenuItems, 
        stringParametersItem);
  }
  
  private void updateMenuItemExistenceForOneMenu(MenuBar menuBar, 
      Collection<ParameterTickItem> menuItems, MenuItem  menuItem) {
    menuBar.clearItems();
    boolean menuIsEmpty = true;
    for (ParameterTickItem item : menuItems) {
      if (enabledAttributesForCurrentSampleFilter.contains(item.attribute())) {
        menuBar.addItem(item.menuItem());
        menuIsEmpty = false;
      }
    }
    menuItem.setEnabled(!menuIsEmpty);
  }
  
  private void updateMenuItemTickedStates(Collection<ParameterTickItem> items, 
      HashSet<Attribute> requiredAttributes, HashSet<Attribute> nonRequiredAttributes) {
    for (ParameterTickItem item : items) {
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
  
  private void fetchAttributesForSampleClass(SampleClass sc) {
    sampleService.attributesForSamples(sc, new PendingAsyncCallback<Attribute[]>(
      SampleSearchScreen.this.manager, "Unable to fetch search parameter.",
      (Attribute[] attribs) -> {
        //Logger.getLogger("aoeu").info("got " + attribs.length + " attributes");
        updateEnabledAttributeSet(attribs);
      }));
  }

  /*
   * Search.Delegate methods
   */
  @Override
  public void searchStarted(Search<?, ?> search) {
    manager.addPendingRequest();
  }

  @Override
  public void searchEnded(Search<?, ?> search, String resultCountText) {
    manager.removePendingRequest();
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

    updateMenuItemExistence();
    
    updateMenuItemTickedStates(numericalParameterMenuItems, requiredAttributes, nonRequiredAttributes);
    updateMenuItemTickedStates(stringParameterMenuItems, requiredAttributes, nonRequiredAttributes);
  }

  @Override
  public void displayDetailsForEntry(Unit unit) {
    SampleDetailTable table = new SampleDetailTable(this, "Unit details", false);
    
    List<Unit> singleUnitList = new ArrayList<Unit>();
    singleUnitList.add(unit);
    Unit[] controls = unitSearch.correspondingControlUnits(singleUnitList);
    Group g = new Group("data", singleUnitList.toArray(new Unit[0]), controls);

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
    getStorage().sampleClassStorage.store(sc);
    fetchAttributesForSampleClass(sc);
  }

  @Override
  public Future<?> filterToolsDatasetsChanged(List<Dataset> datasets) {
    Future<SampleClass[]> future = fetchSampleClasses(new Future<SampleClass[]>(), datasets);
    chosenDatasets = getStorage().datasetsStorage.store(datasets);
    future.addSuccessCallback(sampleClasses -> {
      SampleClass newSampleClass = filterTools.dataFilterEditor.currentSampleClassShowing();
      getStorage().sampleClassStorage.store(newSampleClass);
      fetchAttributesForSampleClass(newSampleClass);
    });
    return future;
  }
}
