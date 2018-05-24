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

package otgviewer.client.components;

import static t.viewer.client.StorageParser.packProbes;
import static t.viewer.client.StorageParser.unpackColumn;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.user.client.ui.*;

import otgviewer.client.Resources;
import otgviewer.client.UIFactory;
import t.common.shared.*;
import t.common.shared.sample.*;
import t.model.SampleClass;
import t.model.sample.AttributeSet;
import t.viewer.client.*;

/**
 * This class is in the process of being phased out in favor of a more lightweight version not
 * deriving from DataListenerWidget. Old comment follows:
 * 
 * Screens are a high level building block for user interfaces. Sequences of screens can form a
 * workflow. Screens require a ScreenManager to assist inter-screen communication. Screens can be
 * hidden/visible and configured/deconfigured. A configured screen has been completely configured by
 * the user, for example by making certain selections. This is a useful concept when late screens
 * depend on data that is selected in earlier screens.
 */
public class DLWScreen extends DataListenerWidget implements Screen,
  RequiresResize, ProvidesResize {

  protected DockLayoutPanel rootPanel;

  /**
   * Each screen is uniquely identified by its key.
   */
  private String key;

  private FlowPanel statusPanel;

  /**
   * Is this screen currently visible?
   */
  protected boolean visible = false;
  private boolean showGroups = false;

  /**
   * Is this screen currently configured?
   */
  protected boolean configured = false;
  private List<MenuItem> menuItems = new ArrayList<MenuItem>();

  /**
   * Widgets to be shown below the main content area, if any.
   */
  private Widget bottom;
  private HorizontalPanel spOuter, guideBar;

  /**
   * Widgets to be shown above the main content area, if any.
   */
  private List<Widget> toolbars = new ArrayList<Widget>();

  /**
   * Widgets to be shown to the left of the main content area, if any. Analogous to "toolbars".
   */
  // private List<Widget> leftbars = new ArrayList<Widget>();

  protected ScreenManager manager;

  protected final Logger logger;

  /**
   * Help text for this screen.
   */
  @Nullable
  protected TextResource helpHTML;
  /**
   * Image to show alongside the help text for this screen.
   */
  @Nullable 
  protected ImageResource helpImage;

  private boolean showGuide;

  @Override
  public Widget widget() {
    return this;
  }

  @Override
  public Logger getLogger() {
    return logger;
  }

  /**
   * An action to be invoked at some later time (for example when data becomes available)
   * 
   * @author johan
   *
   */
  public abstract static class QueuedAction implements Runnable {
    String name;

    public QueuedAction(String name) {
      this.name = name;
    }

    @Override
    public int hashCode() {
      return name.hashCode();
    }

    @Override
    public boolean equals(Object other) {
      if (other instanceof QueuedAction) {
        return name.equals(((QueuedAction) other).name);
      }
      return false;
    }

    @Override
    abstract public void run();
  }

  private List<QueuedAction> actionQueue = new LinkedList<QueuedAction>();

  protected void runActions() {
    for (QueuedAction qa : actionQueue) {
      logger.info("Action queue: run " + qa.name);
      qa.run();
    }
    actionQueue.clear();
  }

  /**
   * Add an action to the queue (or replace an action with the same name). Actions are executed in
   * order, but the order can change if replacement occurs.
   * 
   * @param qa
   */
  @Override
  public void enqueue(QueuedAction qa) {   
    actionQueue.remove(qa); // remove it if it's already there (so we can update it)
    actionQueue.add(qa);
    logger.info("Action queue: added " + qa.name);
  }

  public DLWScreen(String title, String key, boolean showGroups, ScreenManager man,
      @Nullable TextResource helpHTML, @Nullable ImageResource helpImage) {
    this.showGroups = showGroups;
    this.helpHTML = helpHTML;
    this.helpImage = helpImage;

    // PX must be used for measurements or there will be problems in e.g. internet explorer.
    // This problem might possibly be solved if everything is changed to use the new-style
    // LayoutPanels.
    rootPanel = new DockLayoutPanel(Unit.PX);

    initWidget(rootPanel);
    manager = man;
    this.key = key;
    this.logger = SharedUtils.getLogger(key);
    setTitle(title);
  }

  public DLWScreen(String title, String key, boolean showGroups, ScreenManager man) {
    this(title, key, showGroups, man, man.resources().defaultHelpHTML(), null);
  }

  @Override
  public ScreenManager manager() {
    return manager;
  }

  @Override
  public UIFactory factory() {
    return manager.factory();
  }

  @Override
  public DataSchema schema() {
    return manager.schema();
  }

  @Override
  public AttributeSet attributes() {
    return manager.appInfo().attributes();
  }

  @Override
  public Resources resources() {
    return manager.resources();    
  }

  /**
   * Is this screen ready for use?
   * 
   * @return
   */
  @Override
  public boolean enabled() {
    return true;
  }

  /**
   * Has the user finished configuring this screen?
   * 
   * @return
   */
  public boolean configured() {
    return configured;
  }

  /**
   * For subclass implementations to indicate that they have been configured
   */
  @Override
  public void setConfigured(boolean cfg) {
    configured = cfg;
    manager.setConfigured(this, configured);
  }

  /**
   * Subclass implementations should use this method to check whether sufficient state to be
   * "configured" has been loaded. If it has, they should call setConfigured().
   */
  @Override
  public void tryConfigure() {
    setConfigured(true);
  }

  /**
   * Indicate that this screen has finished configuring itself and attempt to display another
   * screen.
   * 
   * @param key
   */
  protected void configuredProceed(String key) {
    setConfigured(true);
    manager.attemptProceed(key);
  }

  @Override
  @Nullable
  public String additionalNavlinkStyle() {
    return null;
  }

  protected HorizontalPanel mkStandardToolbar(Widget content, String styleName) {
    HorizontalPanel r = Utils.mkWidePanel();
    r.setHeight("30px");
    r.add(content);
    r.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
    r.addStyleName(styleName);
    return r;
  }

  @Override
  public void initGUI() {
    statusPanel = new FlowPanel();
    statusPanel.addStyleName("statusPanel");
    Utils.floatLeft(statusPanel);

    spOuter = mkStandardToolbar(statusPanel, "statusPanel");
    guideBar = mkStandardToolbar(mkGuideTools(), "guideBar");

    addToolbars(); // must be called before rootPanel.add()
    bottom = bottomContent();
    if (bottom != null) {
      HorizontalPanel hp = Utils.mkWidePanel();
      hp.add(bottom);
      hp.setHeight("40px");
      rootPanel.addSouth(hp, 40);
    }
    rootPanel.add(content());
  }

  private Widget mkGuideTools() {
    Label l = new Label(getGuideText());
    Utils.floatLeft(l);
    HorizontalPanel hp = Utils.mkWidePanel();
    hp.add(l);

    HorizontalPanel hpi = new HorizontalPanel();

    PushButton i;
    if (helpAvailable()) {
      i = new PushButton(new Image(resources().help()));
      i.setStylePrimaryName("non-gwt-Button"); // just so it doesn't get the GWT button style
      i.addStyleName("slightlySpaced");
      i.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          showHelp();
        }
      });
      hpi.add(i);
    }

    i = new PushButton(new Image(resources().close()));
    i.setStylePrimaryName("non-gwt-Button"); // just so it doesn't get the GWT button style
    i.addStyleName("slightlySpaced");
    i.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        hideToolbar(guideBar);
        showGuide = false;
        storeState();
      }
    });
    hpi.add(i);

    Utils.floatRight(hpi);
    hp.add(hpi);

    return hp;
  }

  @Override
  public void showGuide() {
    showToolbar(guideBar);
    showGuide = true;
    storeState();
  }


  /**
   * This method will be called each time the screen is displayed anew. If overriding, make sure to
   * call the superclass method.
   */
  @Override
  public void show() {
    rootPanel.forceLayout();
    visible = true;
    loadState(attributes());
    if (showGuide) {
      showToolbar(guideBar);
    } else {
      hideToolbar(guideBar);
    }
    updateStatusPanel(); // needs access to the groups from loadState
    runActions();
    deferredResize();
  }

  /**
   * Load saved state from the local storage. If the loaded state is different from what was
   * previously remembered in this widget, the appropriate signals will fire.
   */
  @Override
  public void loadState(AttributeSet attributes) {
    StorageParser p = getParser(this);
    loadState(p, this.schema(), attributes);
  }

  public void loadState(StorageParser parser, DataSchema schema, AttributeSet attributes) {
    SampleClass sc = new SampleClass();
    // Note: currently the "real" sample class, as chosen by the user on the
    // column screen for example, is not stored, and hence not propagated
    // between screens.
    sampleClassChanged(sc);

    try {
      List<Group> cs = loadColumns(parser, schema, "columns", chosenColumns, attributes);
      if (cs != null) {
        logger.info("Unpacked columns: " + cs.get(0) + ": "
            + cs.get(0).getSamples()[0] + " ... ");
        columnsChanged(cs);
      }
      Group g = unpackColumn(schema, parser.getItem("customColumn"), attributes);
      if (g != null) {
        customColumnChanged(g);
      }
    } catch (Exception e) {
      logger.log(Level.WARNING, "Unable to load state", e);
      // one possible failure source is if data is stored in an incorrect
      // format
      columnsChanged(new ArrayList<Group>());
      storeColumns(parser); // overwrite the old data
      storeCustomColumn(parser, null); // ditto
      logger.log(Level.WARNING, "Exception while parsing state", e);
    }

    String probeString = parser.getItem("probes");
    if (probeString != null && !probeString.equals("") &&
        !probeString.equals(packProbes(chosenProbes))) {
      chosenProbes = probeString.split("###");
      probesChanged(chosenProbes);
    } else if (probeString == null || probeString.equals("")) {
      probesChanged(new String[0]);
    }
    List<ItemList> lists = loadItemLists(parser);
    if (lists.size() > 0) {
      chosenItemLists = lists;
      itemListsChanged(lists);
    }

    ItemList geneSet = ItemList.unpack(parser.getItem("geneset"));
    if (geneSet != null) {
      chosenGeneSet = geneSet;
    }
    geneSetChanged(geneSet);

    lists = loadClusteringLists(parser);
    if (lists.size() > 0) {
      chosenClusteringList = lists;
      clusteringListsChanged(lists);
    }

    // Note: the ordering of the following 3 is important
    loadDatasets(parser);
    loadSampleClass(parser, attributes);
    loadCompounds(parser);

    String verbose = parser.getItem("OTG.showGuide");
    if (verbose == null || verbose.equals("yes")) {
      showGuide = true;
    } else {
      showGuide = false;
    }
  }

  public void storeState() {
    storeState(getParser());
  }

  // Storage code moved from DataListenerWidget below

  public void storeState(StorageParser p) {
    storeColumns(p);
    storeProbes(p);
    storeGeneSet(p);
    if (showGuide) {
      p.setItem("OTG.showGuide", "yes");
    } else {
      p.setItem("OTG.showGuide", "no");
    }
  }
  
  @Override
  protected void changeColumns(List<Group> columns) {
    super.changeColumns(columns);
    if (visible) {
      updateStatusPanel();
    }
  }

  protected void updateStatusPanel() {
    // statusPanel.setWidth(Window.getClientHeight() + "px");
    statusPanel.clear();
    if (showGroups) {
      Collections.sort(chosenColumns);
      Utils.addAndFloatLeft(statusPanel, factory().groupLabels(this, schema(), chosenColumns));
    }
  }

  @Override
  public void resizeInterface() {
    for (Widget w : toolbars) {
      rootPanel.setWidgetSize(w, w.getOffsetHeight());
    }
    // for (Widget w: leftbars) {
    // rootPanel.setWidgetSize(w, w.getOffsetWidth());
    // }
    rootPanel.forceLayout();
  }

  protected boolean shouldShowStatusBar() {
    return true;
  }

  /**
   * This can be overridden by subclasses to add more toolbars or more "leftbars".
   */
  protected void addToolbars() {
    addToolbar(guideBar, 40);
    if (!showGuide) {
      guideBar.setVisible(false);
    }
    if (shouldShowStatusBar()) {
      addToolbar(spOuter, 40);
    }
  }

  protected void addToolbar(Widget toolbar, int size) {
    toolbars.add(toolbar);
    rootPanel.addNorth(toolbar, size);
  }

  /**
   * Show the given toolbar (which must previously have been added with addToolbar or addLeftbar at
   * the right time).
   * 
   * @param toolbar
   */
  public void showToolbar(Widget toolbar) {
    showToolbar(toolbar, toolbar.getOffsetHeight());
  }

  /**
   * Show the given toolbar (which must previously have been added with addToolbar or addLeftbar at
   * the right time).
   * 
   * @param toolbar
   * @param size
   */
  public void showToolbar(Widget toolbar, int size) {
    toolbar.setVisible(true);
    rootPanel.setWidgetSize(toolbar, size);
    deferredResize();
  }

  public void hideToolbar(Widget toolbar) {
    toolbar.setVisible(false);
    deferredResize();
  }

  protected void addLeftbar(Widget leftbar, int size) {
    // leftbars.add(leftbar);
    rootPanel.addWest(leftbar, size);
  }

  /**
   * Sometimes we need to do a deferred resize, because the layout engine has not finished yet at
   * the time when we request the resize operation.
   */
  private void deferredResize() {
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      @Override
      public void execute() {
        resizeInterface();
      }
    });
  }

  /**
   * This method will be called each time the screen is hidden. If overriding, make sure to call the
   * superclass method.
   */
  @Override
  public void hide() {
    visible = false;
  }

  public void addMenu(MenuItem m) {
    menuItems.add(m);
  }

  @Override
  public List<MenuItem> menuItems() {
    return menuItems;
  }

  /**
   * Override this method to define the main content of the screen. Stored state may not have been
   * loaded when this method is invoked.
   * 
   * @return
   */
  public Widget content() {
    return new SimplePanel();
  }

  public Widget bottomContent() {
    return null;
  }

  @Override
  public String key() {
    return key;
  }

  @Override
  public StorageParser getParser() {
    return getParser(this);
  }

  public boolean helpAvailable() {
    return helpHTML != null;
  }

  @Override
  public void showHelp() {
    Utils.showHelp(getHelpHTML(), getHelpImage());
  }

  protected TextResource getHelpHTML() {
    if (helpHTML == null) {
      return resources().defaultHelpHTML();
    } else {
      return helpHTML;
    }
  }

  protected ImageResource getHelpImage() {
    return helpImage;
  }

  /**
   * The text that is displayed to first-time users on each screen to assist them.
   * 
   * @return
   */
  protected String getGuideText() {
    return "Use Instructions on the Help menu to get more information.";
  }

  @Override
  public void onResize() {
    final int c = rootPanel.getWidgetCount();
    for (int i = 0; i < c; ++i) {
      Widget w = rootPanel.getWidget(i);
      if (w instanceof RequiresResize) {
        ((RequiresResize) w).onResize();
      }
    }
  }
  
  /**
   * Persisted items that are to be applied once at application startup.
   */
  public List<PersistedState<?>> getPersistedItems() {
    return new ArrayList<>();
  }

  @Override
  public void loadPersistedState() {
    for (PersistedState<?> ps: getPersistedItems()) {
      ps.loadAndApply(getParser());
    }
  }
    
  /**
   * Display the sample detail screen and show information about the given barcode. 
   */
  public void displaySampleDetail(Sample b) {
    ScreenUtils.displaySampleDetail(this, b);    
  }

  private int numPendingRequests = 0;

  private DialogBox waitDialog;

  // Load indicator handling
  @Override
  public void addPendingRequest() {
    numPendingRequests += 1;
    if (numPendingRequests == 1) {
      if (waitDialog == null) {
        waitDialog = Utils.waitDialog();
      } else {
        waitDialog.setPopupPositionAndShow(Utils.displayInCenter(waitDialog));
      }
    }
  }

  @Override
  public void removePendingRequest() {
    numPendingRequests -= 1;
    if (numPendingRequests == 0) {
      waitDialog.hide();
    }
  }

  public void propagateTo(DataViewListener other) {
    other.datasetsChanged(chosenDatasets);
    other.sampleClassChanged(chosenSampleClass);
    other.probesChanged(chosenProbes);
    other.compoundsChanged(chosenCompounds);
    other.columnsChanged(chosenColumns);
    other.customColumnChanged(chosenCustomColumn);
    other.itemListsChanged(chosenItemLists);
    other.geneSetChanged(chosenGeneSet);
    other.clusteringListsChanged(chosenClusteringList);
  }

  protected StorageParser getParser(Screen s) {
    return s.manager().getParser();
  }

  protected void storeColumns(StorageParser p, String key, Collection<? extends SampleColumn> columns) {
    p.storeColumns(key, columns);
  }

  public void storeColumns(StorageParser p) {
    storeColumns(p, "columns", chosenColumns);
  }

  public static void storeCustomColumn(StorageParser p, DataColumn<?> column) {
    p.storeCustomColumn(column);
  }

  // Separator hierarchy for columns:
  // ### > ::: > ^^^ > $$$
  protected List<Group> loadColumns(StorageParser p, DataSchema schema, String key,
      Collection<? extends SampleColumn> expectedColumns, AttributeSet attributes) throws Exception {
    List<Group> storedColumns = p.getColumns(schema, key, attributes);
    if (storedColumns == null
        || StorageParser.packColumns(storedColumns).equals(StorageParser.packColumns(expectedColumns))) {
      return null;
    } else {
      return storedColumns;
    }
  }

  public void storeProbes(StorageParser p) {
    p.setItem("probes", packProbes(chosenProbes));
  }

  public void storeItemLists(StorageParser p) {
    p.storeItemLists(chosenItemLists);
  }

  public void storeGeneSet(StorageParser p) {
    p.setItem("geneset", (chosenGeneSet != null ? chosenGeneSet.pack() : ""));
  }

  public void storeClusteringLists(StorageParser p) {
    p.setItem("clusterings", StorageParser.packItemLists(chosenClusteringList, "###"));
  }

  private String packCompounds(StorageParser p) {
    return StorageParser.packList(chosenCompounds, "###");
  }

  public void storeCompounds(StorageParser p) {
    p.setItem("compounds", packCompounds(p));
  }

  public void storeDatasets(StorageParser p) {
    p.storeDatasets(chosenDatasets);
  }

  public void storeSampleClass(StorageParser p) {
    if (chosenSampleClass != null) {
      p.storeSampleClass(chosenSampleClass);
    }
  }

  public List<ItemList> loadItemLists(StorageParser p) {
    return p.getLists("lists");
  }

  public List<ItemList> loadClusteringLists(StorageParser p) {
    return p.getLists("clusterings");
  }

  public List<ItemList> loadLists(StorageParser p, String name) {
    return p.getLists(name);
  }

  public void loadDatasets(StorageParser p) {
    Dataset[] storedDatasets = p.getDatasets();
    if (storedDatasets == null
        || StorageParser.packDatasets(storedDatasets).equals(StorageParser.packDatasets(chosenDatasets))) {
      return;
    }
    changeDatasets(storedDatasets);
  }

  public void loadCompounds(StorageParser p) {
    List<String> storedCompounds = p.getCompounds();
    if (storedCompounds == null || p.packCompounds(storedCompounds).equals(packCompounds(p))) {
      return;
    }
    changeCompounds(storedCompounds);
  }

  public void loadSampleClass(StorageParser p, AttributeSet attributes) {
    SampleClass storedClass = p.getSampleClass(attributes);
    if (storedClass != null && !t.common.client.Utils.packSampleClass(storedClass)
        .equals(t.common.client.Utils.packSampleClass(chosenSampleClass))) {
      changeSampleClass(storedClass);
    }
  }
}
