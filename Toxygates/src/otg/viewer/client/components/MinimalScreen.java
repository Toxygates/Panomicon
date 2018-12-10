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

package otg.viewer.client.components;

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

import t.common.shared.SharedUtils;
import t.common.shared.sample.Group;
import t.model.sample.AttributeSet;
import t.viewer.client.Utils;

public abstract class MinimalScreen implements Screen {
    
  private String title;

  @Override
  public String getTitle() {
    return title;
  }

  protected void storeState() {
    if (showGuide) {
      getStorage().setItem("OTG.showGuide", "yes");
    } else {
      getStorage().setItem("OTG.showGuide", "no");
    }
  }

  /**
   * Load saved state from local storage.
   */
  @Override
  public void loadState(AttributeSet attributes) {

  }

  // Converted from updateStatusPanel. Rather than setting a showGroups flag, it's now 
  // each screen's responsibility to call this if/when appropriate.
  protected void displayStatusPanel(List<Group> groups) {
    // statusPanel.setWidth(Window.getClientHeight() + "px");
    statusPanel.clear();
    spOuter.setHeight("30px");
    Collections.sort(groups);
    Utils.addAndFloatLeft(statusPanel, factory().groupLabels(this, schema(), groups));
  }

  protected DockLayoutPanel screenPanel;

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
    return screenPanel;
  }

  @Override
  public Logger getLogger() {
    return logger;
  }

  public MinimalScreen(String title, String key, ScreenManager man,
      @Nullable TextResource helpHTML, @Nullable ImageResource helpImage) {
    this.helpHTML = helpHTML;
    this.helpImage = helpImage;

    // PX must be used for measurements or there will be problems in e.g. internet explorer.
    // This problem might possibly be solved if everything is changed to use the new-style
    // LayoutPanels.
    screenPanel = new DockLayoutPanel(Unit.PX);

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

  /**
   * Is this screen ready for use?
   */
  @Override
  public boolean enabled() {
    return true;
  }

  /**
   * Has the user finished configuring this screen?
   */
  public boolean configured() {
    return configured;
  }

  @Override
  @Nullable
  public String additionalNavlinkStyle() {
    return null;
  }

  protected HorizontalPanel mkStandardToolbar(Widget content, String styleName) {
    HorizontalPanel r = Utils.mkWidePanel();
    r.add(content);
    r.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
    r.addStyleName(styleName);
    return r;
  }

  /**
   * This method is responsible for populating the screenPanel with appropriate toolbars,
   * side bars, and main content.
   * Normally this method is only called once. rebuild() may optionally be 
   * called to force the screen to reconstruct itself.
   */
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
      screenPanel.addSouth(hp, 40);
    }
    screenPanel.add(content());
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
    screenPanel.forceLayout();
    visible = true;
    if (showGuide) {
      showToolbar(guideBar);
    } else {
      hideToolbar(guideBar);
    }
    deferredResize();
  }

//  Stubs stored for future reference
//  protected void changeColumns(List<Group> columns) {

  @Override
  public void resizeInterface() {
    for (Widget w : toolbars) {
      screenPanel.setWidgetSize(w, w.getOffsetHeight());
    }
    // for (Widget w: leftbars) {
    // rootPanel.setWidgetSize(w, w.getOffsetWidth());
    // }
    screenPanel.forceLayout();
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
    screenPanel.addNorth(toolbar, size);
  }

  /**
   * Show the given toolbar (which must previously have been added with addToolbar or addLeftbar at
   * the right time).
   */
  @Override
  public void showToolbar(Widget toolbar) {
    showToolbar(toolbar, toolbar.getOffsetHeight());
  }

  /**
   * Show the given toolbar (which must previously have been added with addToolbar or addLeftbar at
   * the right time).
   */
  public void showToolbar(Widget toolbar, int size) {
    toolbar.setVisible(true);
    screenPanel.setWidgetSize(toolbar, size);
    deferredResize();
  }

  @Override
  public void hideToolbar(Widget toolbar) {
    toolbar.setVisible(false);
    deferredResize();
  }

  protected void addLeftbar(Widget leftbar, int size) {
    // leftbars.add(leftbar);
    screenPanel.addWest(leftbar, size);
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

  protected void addMenu(MenuItem m) {
    menuItems.add(m);
  }

  @Override
  public List<MenuItem> menuItems() {
    return menuItems;
  }

  /**
   * Subclasses should override this method to define the main content of the screen. 
   * Stored state may not have been loaded when this method is invoked.
   */
  abstract protected Widget content();
    

  protected Widget bottomContent() {
    return null;
  }

  @Override
  public String key() {
    return key;
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
    } else if (numPendingRequests < 0) {
      numPendingRequests = 0;
      throw new RuntimeException("Tried to remove pending request while numPendingRequests <= 0");
    }
  }
  
  /**
   * Rebuild the GUI of this screen completely.
   * As this is not part of the standard lifecycle, not every screen needs to support
   * this method.
   */
  protected void rebuild() {
    toolbars.clear();
    menuItems.clear();
    screenPanel.clear();
    initGUI();
  }
}
