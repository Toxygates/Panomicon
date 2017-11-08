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

package otgviewer.client;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import otgviewer.client.components.*;
import otgviewer.client.components.Screen.QueuedAction;
import otgviewer.client.dialog.FeedbackForm;
import otgviewer.client.rpc.ProbeService;
import otgviewer.client.rpc.ProbeServiceAsync;
import otgviewer.client.rpc.SampleService;
import otgviewer.client.rpc.SampleServiceAsync;
import t.common.shared.SharedUtils;
import t.common.shared.sample.*;
import t.viewer.client.*;
import t.viewer.client.dialog.DialogPosition;
import t.viewer.client.dialog.MetadataInfo;
import t.viewer.client.rpc.*;
import t.viewer.shared.AppInfo;

import com.google.gwt.core.client.*;
import com.google.gwt.dom.client.*;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.*;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;

/**
 * The main entry point for Toxygates. The main task of this class is to manage the history
 * mechanism and ensure that the correct screen is being displayed at any given time, as well as
 * provide a facility for inter-screen communication.
 */
abstract public class TApplication implements ScreenManager, EntryPoint {
  private static Resources resources = GWT.create(Resources.class);

  private static SampleServiceAsync sampleService = (SampleServiceAsync) GWT
      .create(SampleService.class);
  private static ProbeServiceAsync probeService = (ProbeServiceAsync) GWT
      .create(ProbeService.class);
  private static MatrixServiceAsync matrixService = (MatrixServiceAsync) GWT
      .create(MatrixService.class);
  private static SeriesServiceAsync seriesService = (SeriesServiceAsync) GWT
      .create(SeriesService.class);
  private static UserDataServiceAsync userDataService = (UserDataServiceAsync) GWT
      .create(UserDataService.class);

  private RootLayoutPanel rootPanel;
  private DockLayoutPanel mainDockPanel;
  protected MenuBar menuBar, toolsMenuBar;

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

  @Override
  public AppInfo appInfo() {
    return appInfo;
  }

  @Override
  public void reloadAppInfo(final AsyncCallback<AppInfo> handler) {
    final Logger l = SharedUtils.getLogger();
    final DialogBox wait = Utils.waitDialog();

    @Nullable
    String existingKey = getParser().getItem("userDataKey");
    probeService.appInfo(existingKey, new AsyncCallback<AppInfo>() {
      @Override
      public void onSuccess(AppInfo result) {
        l.info("Got appInfo");
        wait.hide();
        appInfo = result;
        handler.onSuccess(result);
      }

      @Override
      public void onFailure(Throwable caught) {
        wait.hide();
        l.log(Level.WARNING, "Failed to obtain appInfo", caught);
        handler.onFailure(caught);
      }
    });
  }

  /**
   * This is the entry point method.
   */
  @Override
  public void onModuleLoad() {
    Resources.OtgCssResource css = resources.otgViewerStyle();

    String[] colors = new String[] {css.group0_color(), css.group1_color(), css.group2_color(),
        css.group3_color(), css.group4_color(), css.group5_color(), css.group6_color()};
    SampleGroup.setColors(colors);

    css.ensureInjected();

    reloadAppInfo(new AsyncCallback<AppInfo>() {
      @Override
      public void onSuccess(AppInfo result) {
        setupUIBase();
        prepareScreens();
        loadPersistedState();
      }

      @Override
      public void onFailure(Throwable caught) {
        Window.alert("Failed to obtain application information.");
      }
    });

    Logger l = SharedUtils.getLogger();
    l.info("onModuleLoad() finished");
  }

  protected void setupUIBase() {
    menuBar = setupMenu();
    History.addValueChangeHandler(new ValueChangeHandler<String>() {
      @Override
      public void onValueChange(ValueChangeEvent<String> vce) {
        showScreenForToken(vce.getValue(), false);
      }
    });

    rootPanel = RootLayoutPanel.get();

    Window.addResizeHandler(new ResizeHandler() {
      @Override
      public void onResize(ResizeEvent event) {
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
          @Override
          public void execute() {
            resizeInterface();
          }
        });
      }
    });

    mainDockPanel = new DockLayoutPanel(Unit.PX);
    rootPanel.add(mainDockPanel);

    FlowPanel menuBarPanel = new FlowPanel();
    menuBarPanel.add(menuBar);
    menuBarPanel.addStyleName("menuBarPanel");
    mainDockPanel.addNorth(menuBarPanel, 56);

    HorizontalPanel navOuter = Utils.mkHorizontalPanel();
    navOuter.setWidth("100%");
    navOuter.addStyleName("navOuterPanel");

    navPanel = Utils.mkHorizontalPanel();
    navPanel.addStyleName("navPanel");
    navOuter.add(navPanel);
    mainDockPanel.addNorth(navOuter, 35);
  }

  protected void readURLParameters(Screen scr) {
    readImportedProbes(scr);
    readGroupURLparameters(scr);
  }

  protected void readImportedProbes(final Screen scr) {
    Logger l = SharedUtils.getLogger();
    String[] useProbes = null;

    if (appInfo.importedGenes() != null) {
      String[] igs = appInfo.importedGenes();
      l.info("Probes from appInfo/POST request size: " + igs.length);
      useProbes = igs;
    }

    // appInfo.importedGenes overrides GET parameters
    Map<String, List<String>> params = Window.Location.getParameterMap();
    if (useProbes == null && params.containsKey("probes")) {
      List<String> pl = params.get("probes");
      if (!pl.isEmpty()) {
        useProbes = pl.get(0).split(",");
        l.info("probes from URL size: " + useProbes.length);
      }
    }
    if (useProbes != null && useProbes.length > 0) {
      final String[] pr = useProbes;
      scr.enqueue(new QueuedAction("Set probes from URL/POST") {
        @Override
        public void run() {
          probeService.identifiersToProbes(pr, true, true, false, null,
              new PendingAsyncCallback<String[]>(scr, "Failed to resolve gene identifiers") {
                @Override
                public void handleSuccess(String[] probes) {
                  if (Arrays.equals(probes, scr.state().probes)) {
                    return;
                  }
                  scr.probesChanged(probes);
                  scr.storeState(scr);
                  if (scr instanceof DataScreen) {
                    // Force a data reload
                    ((DataScreen) scr).updateProbes();
                  }
                }
              });
        }
      });
    }

  }

  protected void readGroupURLparameters(final Screen scr) {
    Logger l = SharedUtils.getLogger();
    Map<String, List<String>> params = Window.Location.getParameterMap();

    final List<String[]> useGroups = new ArrayList<String[]>();
    final List<String> groupNames = new ArrayList<String>();
    if (params.containsKey("group")) {
      for (String g : params.get("group")) {
        l.info("Group from URL: " + g);
        String[] spl = g.split(",");
        if (spl.length >= 2) {
          groupNames.add(spl[0]);
          useGroups.add(Arrays.copyOfRange(spl, 1, spl.length));
        }
      }
    }

    if (useGroups.size() > 0) {
      scr.enqueue(new QueuedAction("Set columns from URL") {
        @Override
        public void run() {
          sampleService.samplesById(useGroups, new PendingAsyncCallback<List<Sample[]>>(scr,
              "Failed to look up samples") {
            @Override
            public void handleSuccess(List<Sample[]> samples) {
              int i = 0;
              List<Group> finalGroups = new ArrayList<Group>();
              for (Sample[] ss : samples) {
                Group g = new Group(schema(), groupNames.get(i), ss);
                i += 1;
                finalGroups.add(g);
              }
              if (finalGroups.size() > 0 && !finalGroups.equals(scr.chosenColumns())) {
                scr.columnsChanged(finalGroups);
                scr.storeState(scr);
                if (scr instanceof DataScreen) {
                  // Force a data reload
                  ((DataScreen) scr).updateProbes();
                }
              }
            }
          });
        }
      });
    }
  }

  private void prepareScreens() {
    initScreens(); // Need access to the nav. panel
    showScreenForToken(History.getToken(), true);
    reconfigureAll(pickScreen(History.getToken()));
  }

  protected static Storage tryGetStorage() {
    Storage r = Storage.getLocalStorageIfSupported();
    if (r == null) {
      Window
          .alert("Local storage must be supported in the web browser. The application cannot continue.");
    }
    return r;
  }

  private StorageParser parser;

  protected String storageParserPrefix() {
    return instanceName();
  }

  @Override
  public StorageParser getParser() {
    if (parser != null) {
      return parser;
    }
    parser = new StorageParser(tryGetStorage(), storageParserPrefix());
    return parser;
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
   * In the HTML head, e.g.: <meta name="instanceName" content="toxygates"> .
   * 
   * TODO: this information is also available via AppInfo and we should move towards using only
   * that.
   * 
   * @return
   */
  @Deprecated
  protected String instanceName() {
    String v = getMeta("instanceName");
    return v != null ? v : "default";
  }

  protected MenuBar setupMenu() {
    MenuBar menuBar = new MenuBar(false);
    menuBar.setWidth("100%");

    toolsMenuBar = new MenuBar(true);
    MenuItem mi = new MenuItem("Tools", toolsMenuBar);
    postMenuItems.add(mi);

    setupToolsMenu(toolsMenuBar);

    MenuBar hm = new MenuBar(true);
    mi = new MenuItem("Help / feedback", hm);
    postMenuItems.add(mi);
    mi.getElement().setId("helpMenu");

    mi = new MenuItem("Leave feedback...", new Command() {
      @Override
      public void execute() {
        FeedbackForm feedbackDialog =
            new FeedbackForm(currentScreen, currentScreen,
                "kenji@nibiohn.go.jp, y-igarashi@nibiohn.go.jp or jtnystrom@gmail.com");
        feedbackDialog.display("Leave feedback", DialogPosition.Center);
      }
    });
    hm.addItem(mi);
    mi.addStyleName("feedbackMenuItem");

    hm.addItem(new MenuItem("Help for this screen...", new Command() {
      @Override
      public void execute() {
        currentScreen.showHelp();
      }
    }));

    hm.addItem(new MenuItem("Data sources...", new Command() {
      @Override
      public void execute() {
        showDataSources();
      }
    }));

    hm.addItem(new MenuItem("Download user guide...", new Command() {
      @Override
      public void execute() {
        Window.open(appInfo.userGuideURL(), "_blank", "");
      }
    }));

    hm.addItem(new MenuItem("Display guide messages", new Command() {
      @Override
      public void execute() {
        currentScreen.showGuide();
      }
    }));

    hm.addItem(new MenuItem("About Toxygates...", new Command() {
      @Override
      public void execute() {
        Utils.showHelp(getAboutHTML(), getAboutImage());
      }
    }));

    hm.addItem(new MenuItem("Version history...", new Command() {
      @Override
      public void execute() {
        Utils.showHelp(getVersionHTML(), null);
      }
    }));

    return menuBar;
  }

  protected void setupToolsMenu(MenuBar toolsMenuBar) {

  }

  protected void showDataSources() {
    VerticalPanel vp = new VerticalPanel();
    vp.add(MetadataInfo.fromPlatforms(appInfo.platforms()));
    vp.add(MetadataInfo.annotations(appInfo));
    Utils.displayInPopup("Data sources information", vp, DialogPosition.Center);
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
      // String link = (i < workflow.size() - 1) ? (s.getTitle() + " >> ") : s.getTitle();
      String link = s.getTitle();
      final Label label = new Label(link);
      label.setStylePrimaryName("navlink");
      if (s.enabled() && s != current) {
        label.addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent e) {
            History.newItem(s.key());
          }
        });
        label.addStyleDependentName("enabled");
      } else {
        if (s == current) {
          label.addStyleDependentName("current");
        } else {
          label.addStyleDependentName("disabled");
        }
      }
      if (i == 0) {
        label.addStyleDependentName("first");
      }
      navPanel.add(label);
    }
  }

  /**
   * Display the screen that corresponds to a given history token.
   * 
   * @param token
   */
  private void showScreenForToken(String token, boolean firstLoad) {
    Screen s = pickScreen(token);
    if (firstLoad) {
      readURLParameters(s);
    }
    showScreen(s);
    Analytics.trackPageView(Analytics.URL_PREFIX + token);
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
        toolsMenuBar.removeItem(mi);
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
      toolsMenuBar.addItem(mi);
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
  @Override
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
  public void reconfigureAll(Screen from) {
    for (Screen s : workflow) {
      if (s != from) {
        s.setConfigured(false);
      }
    }
    for (Screen s : workflow) {
      if (s != from) {
        s.loadState(s, appInfo.attributes());
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

  @Override
  public Resources resources() {
    return resources;
  }

  @Override
  public SampleServiceAsync sampleService() {
    return sampleService;
  }

  @Override
  public ProbeServiceAsync probeService() {
    return probeService;
  }

  @Override
  public SeriesServiceAsync seriesService() {
    return seriesService;
  }

  @Override
  public MatrixServiceAsync matrixService() {
    return matrixService;
  }

  @Override
  public UserDataServiceAsync userDataService() {
    return userDataService;
  }

  /**
   * Persisted items that are to be applied at application startup,
   * when all screens have been initialised.
   */
  protected List<PersistedState<?>> getPersistedItems() {
    return new ArrayList<>();
  }

  protected void loadPersistedState() {
    for (PersistedState<?> ps: getPersistedItems()) {
      ps.loadAndApply(getParser());
    }
    for (Screen s: screens.values()) {
      s.loadPersistedState();
    }
  }
  
}
