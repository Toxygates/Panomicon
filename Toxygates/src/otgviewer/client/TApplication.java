/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health
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

package otgviewer.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import otgviewer.client.components.Screen;
import otgviewer.client.components.ScreenManager;
import otgviewer.client.dialog.FeedbackForm;
import otgviewer.client.dialog.PlatformInfo;
import otgviewer.client.targetmine.TargetMineData;
import t.common.shared.SharedUtils;
import t.viewer.client.Utils;
import t.viewer.client.dialog.DialogPosition;
import t.viewer.client.rpc.MatrixService;
import t.viewer.client.rpc.MatrixServiceAsync;
import t.viewer.client.rpc.SeriesService;
import t.viewer.client.rpc.SeriesServiceAsync;
import t.viewer.client.rpc.SparqlService;
import t.viewer.client.rpc.SparqlServiceAsync;
import t.viewer.shared.AppInfo;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.MetaElement;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * The main entry point for Toxygates. The main task of this class is to manage the history
 * mechanism and ensure that the correct screen is being displayed at any given time, as well as
 * provide a facility for inter-screen communication.
 * 
 * @author johan
 *
 */
abstract public class TApplication implements ScreenManager, EntryPoint {
  private static Resources resources = GWT.create(Resources.class);

  private static SparqlServiceAsync sparqlService = (SparqlServiceAsync) GWT
      .create(SparqlService.class);
  private static MatrixServiceAsync matrixService = (MatrixServiceAsync) GWT
      .create(MatrixService.class);
  private static SeriesServiceAsync seriesService = (SeriesServiceAsync) GWT
      .create(SeriesService.class);

  private RootLayoutPanel rootPanel;
  private DockLayoutPanel mainDockPanel;
  protected MenuBar menuBar, analysisMenuBar;

  // Menu items to be shown to the left of menu items belonging to the current screen.
  protected List<MenuItem> preMenuItems = new LinkedList<MenuItem>();

  // Menu items to be shown to the right of menu items belonging to the current screen.
  protected List<MenuItem> postMenuItems = new LinkedList<MenuItem>();

  private HorizontalPanel navPanel;

  /**
   * All screens in the order that the links are displayed at the top.
   */
  private List<Screen> workflow = new ArrayList<Screen>();

  /**
   * All available screens. The key in this map is the "key" field of each Screen instance, which
   * also corresponds to the history token used with GWT's history tracking mechanism.
   */
  protected Map<String, Screen> screens = new HashMap<String, Screen>();

  /**
   * All currently configured screens. See the Screen class for an explanation of the "configured"
   * concept.
   */
  private Set<String> configuredScreens = new HashSet<String>();

  /**
   * The screen currently being displayed.
   */
  protected Screen currentScreen;

  protected final Logger logger = SharedUtils.getLogger("application");

  protected AppInfo appInfo = null;

  public AppInfo appInfo() {
    return appInfo;
  }

  /**
   * This is the entry point method.
   */
  public void onModuleLoad() {
    menuBar = setupMenu();

    History.addValueChangeHandler(new ValueChangeHandler<String>() {
      public void onValueChange(ValueChangeEvent<String> vce) {
        setScreenForToken(vce.getValue());
      }
    });

    rootPanel = RootLayoutPanel.get();

    Window.addResizeHandler(new ResizeHandler() {
      public void onResize(ResizeEvent event) {
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
          public void execute() {
            resizeInterface();
          }
        });
      }
    });

    mainDockPanel = new DockLayoutPanel(Unit.PX);
    rootPanel.add(mainDockPanel);

    mainDockPanel.addNorth(menuBar, 35);

    navPanel = Utils.mkHorizontalPanel();
    mainDockPanel.addNorth(navPanel, 35);

    final Logger l = SharedUtils.getLogger();
    final DialogBox wait = Utils.waitDialog();

    sparqlService.appInfo(new AsyncCallback<AppInfo>() {
      @Override
      public void onSuccess(AppInfo result) {
        l.info("Got appInfo");
        appInfo = result;
        wait.hide();
        prepareScreens();
      }

      @Override
      public void onFailure(Throwable caught) {
        wait.hide();
        Window.alert("Failed to obtain application information.");
        l.log(Level.WARNING, "Failed to obtain appInfo", caught);
      }
    });

    l.info("onModuleLoad() finished");
  }

  private void prepareScreens() {
    initScreens(); // Need access to the nav. panel

    setScreenForToken(History.getToken());
    deconfigureAll(pickScreen(History.getToken()));
  }

  public String storagePrefix() {
    return instanceName();
  }

  private @Nullable String getMeta(String key) {
    NodeList<Element> metas = Document.get().getElementsByTagName("meta");
    for (int i = 0; i < metas.getLength(); i++) {
      MetaElement meta = (MetaElement) metas.getItem(i);
      if (key.equals(meta.getName())) {
        return meta.getContent();
      }
    }
    return null;
  }

  /**
   * A <meta> tag in the hosting HTML page identifies the kind of UI we want to show in this
   * application. E.g.: <meta name="uitype" content="toxygates"> . TODO: retire
   * 
   * @return The requested UI type
   */
  @Deprecated
  public String getUIType() {
    String v = getMeta("uitype");
    return v != null ? v : "toxygates";
  }

  /**
   * E.g.: <meta name="instanceName" content="toxygates"> .
   * 
   * @return
   */
  protected String instanceName() {
    String v = getMeta("instanceName");
    return v != null ? v : "default";
  }

  protected MenuBar setupMenu() {
    MenuBar menuBar = new MenuBar(false);
    menuBar.setWidth("100%");

    analysisMenuBar = new MenuBar(true);
    MenuItem mi = new MenuItem("Tools", analysisMenuBar);
    postMenuItems.add(mi);

    MenuBar targetmineMenu = new MenuBar(true);
    mi = new MenuItem("TargetMine data", targetmineMenu);

    targetmineMenu.addItem(new MenuItem("Import gene lists from TargetMine...", new Command() {
      public void execute() {
        new TargetMineData(currentScreen).importLists(true);
      }
    }));

    targetmineMenu.addItem(new MenuItem("Export gene lists to TargetMine...", new Command() {
      public void execute() {
        new TargetMineData(currentScreen).exportLists();
      }
    }));

    targetmineMenu.addItem(new MenuItem("Enrichment...", new Command() {
      public void execute() {
        new TargetMineData(currentScreen).enrich();
      }
    }));

    targetmineMenu.addItem(new MenuItem("Go to TargetMine", new Command() {
      public void execute() {
        Utils.displayURL("Go to TargetMine in a new window?", "Go", appInfo.targetmineURL());
      }
    }));
    analysisMenuBar.addItem(mi);

    mi = new MenuItem("Rank compounds...", new Command() {
      public void execute() {
        ColumnScreen cs = (ColumnScreen) screens.get("columns");
        if (cs.enabled()) {
          showScreen(cs);
          cs.displayCompoundRankUI();
        } else {
          Window.alert("Please select a dataset to rank compounds.");
        }
      }
    });
    analysisMenuBar.addItem(mi);

    MenuBar hm = new MenuBar(true);
    mi = new MenuItem("Help / feedback", hm);
    postMenuItems.add(mi);
    mi.getElement().setId("helpMenu");

    mi = new MenuItem("Leave feedback...", new Command() {
      public void execute() {
        FeedbackForm feedbackDialog = new FeedbackForm(currentScreen, currentScreen);
        feedbackDialog.display("Leave feedback", DialogPosition.Center);
      }
    });
    hm.addItem(mi);
    mi.addStyleName("feedbackMenuItem");

    hm.addItem(new MenuItem("Help for this screen...", new Command() {
      public void execute() {
        currentScreen.showHelp();
      }
    }));
    
    hm.addItem(new MenuItem("Data sources...", new Command() {
      public void execute() {
        showDataSources();
      }
    }));

    hm.addItem(new MenuItem("Download user guide...", new Command() {
      public void execute() {
        Window.open(appInfo.userGuideURL(), "_blank", "");
      }
    }));

    hm.addItem(new MenuItem("Display guide messages", new Command() {
      public void execute() {
        currentScreen.showGuide();
      }
    }));

    hm.addItem(new MenuItem("About Toxygates...", new Command() {
      public void execute() {
        Utils.showHelp(getAboutHTML(), getAboutImage());
      }
    }));

    hm.addItem(new MenuItem("Version history...", new Command() {
      public void execute() {
        Utils.showHelp(getVersionHTML(), null);
      }
    }));

    return menuBar;
  }
  
  protected void showDataSources() {
    Widget info = new PlatformInfo(appInfo.platforms());
    Utils.displayInPopup("Data sources information", info, DialogPosition.Center);
  }

  /**
   * This method sets up the navigation links that allow the user to jump between screens. The
   * enabled() method of each screen is used to test whether that screen is currently available for
   * use or not.
   * 
   * @param current
   */
  void addWorkflowLinks(Screen current) {
    navPanel.clear();
    for (int i = 0; i < workflow.size(); ++i) {
      final Screen s = workflow.get(i);
      String link = (i < workflow.size() - 1) ? (s.getTitle() + " >> ") : s.getTitle();
      Label l = new Label(link);
      if (s.enabled() && s != current) {
        l.addClickHandler(new ClickHandler() {
          public void onClick(ClickEvent e) {
            History.newItem(s.key());
          }
        });
        l.setStylePrimaryName("clickHeading");
      } else {
        if (s == current) {
          l.setStylePrimaryName("headingCurrent");
        } else {
          l.setStylePrimaryName("headingBlack");
        }
      }
      navPanel.add(l);
    }
  }

  /**
   * Display the screen that corresponds to a given history token.
   * 
   * @param token
   */
  private void setScreenForToken(String token) {
    Screen s = pickScreen(token);
    showScreen(s);
  }

  /**
   * Switch screens.
   * 
   * @param s
   */
  protected void showScreen(Screen s) {
    if (currentScreen != null) {
      mainDockPanel.remove(currentScreen);
      currentScreen.hide();
      for (MenuItem mi : s.analysisMenuItems()) {
        analysisMenuBar.removeItem(mi);
      }
    }
    currentScreen = s;
    menuBar.clearItems();
    List<MenuItem> allItems = new LinkedList<MenuItem>(preMenuItems);
    allItems.addAll(s.menuItems());
    allItems.addAll(postMenuItems);

    for (MenuItem mi : allItems) {
      menuBar.addItem(mi);
    }

    for (MenuItem mi : s.analysisMenuItems()) {
      analysisMenuBar.addItem(mi);
    }

    addWorkflowLinks(currentScreen);
    mainDockPanel.add(currentScreen);
    currentScreen.show();
    mainDockPanel.forceLayout(); // IE8
    resizeInterface();
  }

  /**
   * Pick the appropriate screen for a given history token.
   * 
   * @return
   */
  protected Screen pickScreen(String token) {
    if (!screens.containsKey(token)) {
      return screens.get(defaultScreenKey()); // default
    } else {
      return screens.get(token);
    }
  }

  protected String defaultScreenKey() {
    return StartScreen.key;
  }

  /**
   * Proceed if the screen is ready.
   */
  public void attemptProceed(String to) {
    Screen s = pickScreen(to);
    if (s.enabled()) {
      History.newItem(to);
    } else {
      // proceed to default screen (must always be enabled!)
      History.newItem(defaultScreenKey());
    }
  }

  /**
   * Helper method for initialising screens
   * 
   * @param s
   */
  protected void addScreenSeq(Screen s) {
    logger.info("Configure screen: " + s.getTitle() + " -> " + s.key());
    screens.put(s.key(), s);
    workflow.add(s);
    s.initGUI();
    s.tryConfigure(); // give it a chance to register itself as configured
  }

  /**
   * Set up the workflow sequence once.
   */
  abstract protected void initScreens();

  @Override
  public void setConfigured(Screen s, boolean configured) {
    if (configured) {
      configuredScreens.add(s.key());
    } else {
      configuredScreens.remove(s.key());
    }
  }

  @Override
  public void deconfigureAll(Screen from) {
    for (Screen s : workflow) {
      if (s != from) {
        s.setConfigured(false);
      }
    }
    for (Screen s : workflow) {
      if (s != from) {
        s.loadState(s);
        s.tryConfigure();
      }
    }
    addWorkflowLinks(currentScreen);
  }

  @Override
  public boolean isConfigured(String key) {
    return configuredScreens.contains(key);
  }

  protected TextResource getAboutHTML() {
    return resources.aboutHTML();
  }

  protected ImageResource getAboutImage() {
    return resources.about();
  }

  protected TextResource getVersionHTML() {
    return resources.versionHTML();
  }

  private void resizeInterface() {
    if (currentScreen != null) {
      currentScreen.resizeInterface();
    }
    rootPanel.onResize();
  }

  public Resources resources() {
    return resources;
  }

  public SparqlServiceAsync sparqlService() {
    return sparqlService;
  }

  public SeriesServiceAsync seriesService() {
    return seriesService;
  }

  public MatrixServiceAsync matrixService() {
    return matrixService;
  }

}
