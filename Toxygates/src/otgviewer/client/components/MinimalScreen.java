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

package otgviewer.client.components;

import java.util.*;
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
import otgviewer.client.components.DLWScreen.QueuedAction;
import t.common.shared.*;
import t.common.shared.sample.Sample;
import t.model.sample.AttributeSet;
import t.viewer.client.*;

/**
 * Screen implementation based on DLWScreen. Instead of inheriting from DataListenerWidget, we
 * delegate data-related calls to a delegate object.
 * 
 * This is meant to become the canonical implementation of Screen.
 */
public class MinimalScreen implements Screen {
  public interface ScreenDelegate {
    void storeState();
    void loadState(AttributeSet attributes);
    ClientState state();

    void intermineImport(List<ItemList> itemLists, List<ItemList> clusteringLists);
  }
  
  private ScreenDelegate delegate;
    
  private String title;

  @Override
  public String getTitle() {
    return title;
  }

  @Override
  public ClientState state() {
    return delegate.state();
  }

  /*
   * Copy-pasted (and lightly modified) DLWScreen follows below.
   * 
   * Major changes so far: 
   * * Not doing status panel 
   * * Not overriding Widget.OnResize (probably problematic)
   */

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
  // private boolean showGroups = false;

  /**
   * Is this screen currently configured?
   */
  protected boolean configured = false;
  private List<MenuItem> menuItems = new ArrayList<MenuItem>();
  private List<MenuItem> analysisMenuItems = new ArrayList<MenuItem>();


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
    return rootPanel;
  }

  @Override
  public Logger getLogger() {
    return logger;
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

  public MinimalScreen(String title, String key, ScreenManager man,
      @Nullable TextResource helpHTML, @Nullable ImageResource helpImage) {
    this.helpHTML = helpHTML;
    this.helpImage = helpImage;

    // PX must be used for measurements or there will be problems in e.g. internet explorer.
    // This problem might possibly be solved if everything is changed to use the new-style
    // LayoutPanels.
    rootPanel = new DockLayoutPanel(Unit.PX);

    manager = man;
    this.key = key;
    this.logger = SharedUtils.getLogger(key);
    this.title = title;
  }

  public MinimalScreen(String title, String key, ScreenManager man) {
    this(title, key, man, man.resources().defaultHelpHTML(), null);
  }

  @Override
  public ScreenManager manager() {
    return manager;
  }

  public UIFactory factory() {
    return manager.factory();
  }

  public DataSchema schema() {
    return manager.schema();
  }

  public AttributeSet attributes() {
    return manager.appInfo().attributes();
  }

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
        delegate.storeState();
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
    delegate.storeState();
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
    // Consider reinstating later
    //updateStatusPanel(); // needs access to the groups from loadState
    runActions();
    deferredResize();
  }

  /**
   * Load saved state from the local storage. If the loaded state is different from what was
   * previously remembered in this widget, the appropriate signals will fire.
   */
  @Override
  public void loadState(AttributeSet attributes) {
    delegate.loadState(attributes);
  }

//  Stubs stored for future reference

//  public void loadState(StorageParser parser, DataSchema schema, AttributeSet attributes) {

//  public void storeState(StorageParser p) {

//  protected void changeColumns(List<Group> columns) {

//  Consider reinstating later
  /*
   * protected void updateStatusPanel() { // statusPanel.setWidth(Window.getClientHeight() + "px");
   * statusPanel.clear(); if (showGroups) { Collections.sort(chosenColumns);
   * Utils.addAndFloatLeft(statusPanel, factory().groupLabels(this, schema(), chosenColumns)); } }
   */

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
  public void addAnalysisMenuItem(MenuItem mi) {
    analysisMenuItems.add(mi);
  }

  @Override
  public List<MenuItem> menuItems() {
    return menuItems;
  }

  @Override
  public List<MenuItem> analysisMenuItems() {
    return analysisMenuItems;
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

  public StorageParser getParser() {
    return manager().getParser();
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

  /*
   * Not having overridden this will probably cause problems 
   public void onResize() { 
     final int c = rootPanel.getWidgetCount(); 
     for (int i = 0; i < c; ++i) { Widget w = rootPanel.getWidget(i); 
       if (w instanceof RequiresResize) { 
         ((RequiresResize) w).onResize(); 
       } 
     } 
   }
   */

  /**
   * Persisted items that are to be applied once at application startup.
   */
  public List<PersistedState<?>> getPersistedItems() {
    return new ArrayList<>();
  }

  @Override
  public void loadPersistedState() {
    for (PersistedState<?> ps : getPersistedItems()) {
      ps.loadAndApply(getParser());
    }
  }

  /**
   * Display the sample detail screen and show information about the given barcode. TODO: this
   * method should probably be somewhere else.
   * 
   * @param b
   */
  public void displaySampleDetail(Sample b) {
    ScreenUtils.displaySampleDetail(this, b);
  }

  @Override
  public void intermineImport(List<ItemList> itemLists, List<ItemList> clusteringLists) {
    delegate.intermineImport(itemLists, clusteringLists);
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
}
