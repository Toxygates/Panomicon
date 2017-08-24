package otgviewer.client;

import java.util.*;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;

import otgviewer.client.components.*;
import t.common.shared.sample.*;
import t.model.SampleClass;
import t.model.sample.*;
import t.viewer.client.Utils;
import t.viewer.client.components.search.*;
import t.viewer.client.dialog.DialogPosition;
import t.viewer.client.rpc.SampleServiceAsync;
import t.viewer.shared.AppInfo;

public class SampleSearchScreen extends Screen implements Search.Delegate, ResultTable.Delegate {
  public static final String key = "search";

  private AppInfo appInfo;
  private SampleServiceAsync sampleService;

  private FilterTools filterTools;

  private Widget tools;
  private ConditionEditor conditionEditor;
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

  private void getParameterInfo() {
    AttributeSet attributes = appInfo.attributes();
    List<Attribute> searchParams = attributes.getNumerical();
    List<Attribute> nonSearchParams = attributes.getString();
    
    java.util.Collections.sort(searchParams, new AttributeComparator());
    java.util.Collections.sort(nonSearchParams, new AttributeComparator());

    searchParameters = searchParams;
    nonSearchParameters = nonSearchParams;
  }

  public SampleSearchScreen(ScreenManager man) {
    super("Sample search", key, true, man);
    appInfo = man.appInfo();
    filterTools = new FilterTools(this);
    this.addListener(filterTools);

    sampleService = man.sampleService();

    sampleSearch = new SampleSearch(this, sampleTableHelper, appInfo.attributes(), 
      sampleService);
    unitSearch = new UnitSearch(this, unitTableHelper, appInfo.attributes(), 
      sampleService);

    getParameterInfo();

    makeTools();
  }

  private void makeTools() {
    conditionEditor = new ConditionEditor(searchParameters);

    Button sampleSearchButton = new Button("Sample Search");
    sampleSearchButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        sampleSearch.attemptSearch(chosenSampleClass, conditionEditor.getCondition());
      }
    });

    Button unitSearchButton = new Button("Unit Search");
    unitSearchButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        logger.info("Search class:" + chosenSampleClass);
        unitSearch.attemptSearch(chosenSampleClass, conditionEditor.getCondition());
      }
    });

    resultCountLabel = new Label();

    saveGroupButton = new Button("Save sample group", new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        try {
          Unit[] units =
              unitSearch.sampleGroupFromUnits(unitTableHelper.selectionTable().getSelection());
          String name = findAvailableGroupName("Sample search group ");
          Group pendingGroup = new Group(schema(), name, units);

          chosenColumns.add(pendingGroup);
          columnsChanged(chosenColumns);
          storeColumns(manager().getParser());

          unitTableHelper.selectionTable().clearSelection();

          Window.alert("Saved group: " + name);
        } catch (Exception e) {
          Window.alert("Saving group failed: " + e);
        }
      }
    });
    saveGroupButton.setVisible(false);

    tools = Utils.mkVerticalPanel(true, conditionEditor, Utils.mkHorizontalPanel(true,
        unitSearchButton, sampleSearchButton, resultCountLabel, saveGroupButton));
  }

  private String findAvailableGroupName(String prefix) throws Exception {
    List<Group> inactiveGroups =
        loadColumns(manager().getParser(), schema(), "inactiveColumns", new ArrayList<Group>());

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
  public Widget content() {
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
    saveCVSMenuItem = new MenuItem("Save results to CSV", false, new Command() {
      @Override
      public void execute() {
        prepareUnitCVSDownload();
      }
    });
    saveCVSMenuItem.setEnabled(false);
    fileBar.addItem(saveCVSMenuItem);
    addMenu(fileItem);

    MenuBar editBar = new MenuBar(true);
    MenuItem editItem = new MenuItem("Edit", false, editBar);
    MenuItem clearSearchConditionItem =
        new MenuItem("Clear search condition", false, new Command() {
          @Override
          public void execute() {
            conditionEditor.clear();
          }
        });
    editBar.addItem(clearSearchConditionItem);
    MenuItem clearSelectionItem = new MenuItem("Clear selection", false, new Command() {
      @Override
      public void execute() {
        currentSearch.helper().selectionTable().clearSelection();
      }
    });
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
    addToolbar(filterTools, 30);
  }

  @Override
  public String getGuideText() {
    return "Here you can search for samples and units based on values of biological parameters.";
  }

  @Override
  protected boolean shouldShowStatusBar() {
    return false;
  }

  @Override
  public void changeSampleClass(SampleClass sc) {
    super.changeSampleClass(sc);
    storeSampleClass(getParser());
  }

  protected void hideTables() {
    sampleTableHelper.clear();
    sampleTableHelper.selectionTable().setVisible(false);
    unitTableHelper.clear();
    unitTableHelper.selectionTable().setVisible(false);
  }

  private void prepareUnitCVSDownload() {
    SampleSearchScreen.this.sampleService.prepareUnitCSVDownload(unitSearch.searchResult(),
        unitTableHelper.allAttributes(), new PendingAsyncCallback<String>(SampleSearchScreen.this,
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
    saveCVSMenuItem.setEnabled(currentSearch == unitSearch);
    saveGroupButton.setVisible((currentSearch == unitSearch));
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
    Unit[] unitsWithControl = unitSearch.sampleGroupFromUnits(singleUnitList);
    Group g = new Group(schema(), "data", unitsWithControl);

    table.loadFrom(g, false);
    Utils.displayInPopup("Unit details", table, DialogPosition.Top);
  }
}
