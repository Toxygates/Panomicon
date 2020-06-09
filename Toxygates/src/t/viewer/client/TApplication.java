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

package t.viewer.client;

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
import com.google.gwt.event.shared.UmbrellaException;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import t.viewer.client.components.FeedbackForm;
import t.viewer.client.screen.ImportingScreen;
import t.viewer.client.screen.Screen;
import t.viewer.client.screen.ScreenManager;
import t.viewer.client.rpc.SeriesService;
import t.viewer.client.rpc.SeriesServiceAsync;
import t.common.shared.SharedUtils;
import t.common.shared.sample.SampleGroup;
import t.viewer.client.dialog.DialogPosition;
import t.viewer.client.dialog.MetadataInfo;
import t.viewer.client.rpc.*;
import t.viewer.client.screen.StartScreen;
import t.viewer.client.storage.StorageProvider;
import t.viewer.shared.AppInfo;

import javax.annotation.Nullable;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The main entry point for Toxygates. The main task of this class is to manage the history
 * mechanism and ensure that the correct screen is being displayed at any given time, as well as
 * provide a facility for inter-screen communication.
 */
abstract public class TApplication implements ScreenManager, EntryPoint {
  private static Resources resources = GWT.create(Resources.class);

  private static SampleServiceAsync sampleService = GWT
      .create(SampleService.class);
  private static ProbeServiceAsync probeService = GWT
      .create(ProbeService.class);
  private static MatrixServiceAsync matrixService = GWT
      .create(MatrixService.class);
  private static SeriesServiceAsync seriesService = GWT
      .create(SeriesService.class);
  private static UserDataServiceAsync userDataService = GWT
      .create(UserDataService.class);
  private static NetworkServiceAsync networkService = GWT
      .create(NetworkService.class);
  
  private RootLayoutPanel rootPanel;
  private DockLayoutPanel mainDockPanel;
  protected MenuBar menuBar;

  // Menu items to be shown to the left of menu items belonging to the current screen.
  protected List<MenuItem> preMenuItems = new LinkedList<MenuItem>();

  // Menu items to be shown to the right of menu items belonging to the current screen.
  protected List<MenuItem> postMenuItems = new LinkedList<MenuItem>();

  private HorizontalPanel navPanel;

  /**
   * All screens in order of links being displayed at the top
   */
  private List<Screen> screens = new ArrayList<Screen>();
  
  /**
   * All available screens. The key in this map is the "key" field of each Screen instance, which
   * also corresponds to the history token used with GWT's history tracking mechanism.
   */
  protected Map<String, Screen> screensBykey = new HashMap<String, Screen>();

  /**
   * The screen currently being displayed.
   */
  protected Screen currentScreen;

  protected final Logger logger = SharedUtils.getLogger("application");

  protected AppInfo appInfo = null;

  protected Resources.OtgCssResource css = resources.otgViewerStyle();

  protected ImportingScreen importingScreen;

  @Override
  public AppInfo appInfo() {
    return appInfo;
  }

  @Override
  public void reloadAppInfo(final AsyncCallback<AppInfo> handler) {
    final Logger l = SharedUtils.getLogger();
    final DialogBox waitDialog = Utils.waitDialog();
    Utils.displayInCenter(waitDialog);

    // We don't use StorageProvider here, because Storageprovider initialization requires an appInfo
    @Nullable
    String existingKey = tryGetStorage().getItem(storagePrefix() + ".userDataKey");
    probeService.appInfo(existingKey, new AsyncCallback<AppInfo>() {
      @Override
      public void onSuccess(AppInfo result) {
        l.info("Got appInfo");
        waitDialog.hide();
        appInfo = result;
        handler.onSuccess(result);
      }

      @Override
      public void onFailure(Throwable caught) {
        waitDialog.hide();
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
    String[] colors = new String[] {css.group0_color(), css.group1_color(), css.group2_color(),
        css.group3_color(), css.group4_color(), css.group5_color(), css.group6_color(),
        css.group7_color()};
    SampleGroup.setColors(colors);

    css.ensureInjected();

    GWT.setUncaughtExceptionHandler(new GWT.UncaughtExceptionHandler() {      
      @Override
      public void onUncaughtException(Throwable e) {
        Throwable unwrapped = unwrap(e);
        logger.log(Level.WARNING, "Exception", unwrapped);
      }

      public Throwable unwrap(Throwable e) {
        if(e instanceof UmbrellaException) {
          UmbrellaException ue = (UmbrellaException) e;
          if(ue.getCauses().size() == 1) {
            return unwrap(ue.getCauses().iterator().next());
          }
        }
        return e;
      }
    });
    
    reloadAppInfo(new AsyncCallback<AppInfo>() {
      @Override
      public void onSuccess(AppInfo result) {
        setupUIBase();
        prepareScreens();        
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
    mainDockPanel.addNorth(menuBarPanel, css.menubarpanel_height());

    HorizontalPanel navOuter = Utils.mkHorizontalPanel();
    navOuter.setWidth("100%");
    navOuter.addStyleName("navOuterPanel");

    navPanel = Utils.mkHorizontalPanel();
    navPanel.addStyleName("navPanel");
    navOuter.add(navPanel);
    mainDockPanel.addNorth(navOuter, css.navpanel_height());
  }

  protected boolean readURLParameters() {
    return readImportedProbes();
  }

  protected boolean readImportedProbes() {
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
      importingScreen.setUrlProbes(useProbes);
      return true;
    } else {
      return false;
    }

  }

  private void prepareScreens() {
    initScreens(); // Need access to the nav. panel
    showScreenForToken(History.getToken(), true);
    resetWorkflowLinks();
  }

  protected static Storage tryGetStorage() {
    Storage r = Storage.getLocalStorageIfSupported();
    if (r == null) {
      Window
          .alert("Local storage must be supported in the web browser. The application cannot continue.");
    }
    return r;
  }

  private StorageProvider storage;

  protected String storagePrefix() {
    return instanceName();
  }

  @Override
  public StorageProvider getStorage() {
    if (storage != null) {
      return storage;
    }
    storage = new StorageProvider(tryGetStorage(), storagePrefix(), schema(), appInfo());
    return storage;
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
   * In the HTML head, e.g.: <meta name="instanceName" content="toxygates"> . Currently used only
   * when loading the initial AppInfo (after which instanceName may be obtained from there).
   */
  private String instanceName() {
    String v = getMeta("instanceName");
    return v != null ? v : "default";
  }

  protected MenuBar setupMenu() {
    MenuBar menuBar = new MenuBar(false);
    menuBar.setWidth("100%");

    MenuBar hm = new MenuBar(true);
    MenuItem mi = new MenuItem("Help / feedback", hm);
    postMenuItems.add(mi);
    mi.getElement().setId("helpMenu");

    mi = new MenuItem("Leave feedback...", () -> {      
        FeedbackForm feedbackDialog =
          new FeedbackForm(currentScreen,
                "kenji@nibiohn.go.jp, y-igarashi@nibiohn.go.jp or jtnystrom@gmail.com");
        feedbackDialog.display("Leave feedback", DialogPosition.Center);
      });
    
    hm.addItem(mi);
    mi.addStyleName("feedbackMenuItem");

    hm.addItem(new MenuItem("Help for this screen...", () ->      
        currentScreen.showHelp()));

    hm.addItem(new MenuItem("Data sources...", () -> showDataSources()));

    hm.addItem(new MenuItem("Download user guide...", () ->      
        Window.open(appInfo.userGuideURL(), "_blank", "")
      ));

    hm.addItem(new MenuItem("Display guide messages", () -> currentScreen.showGuide()));

    hm.addItem(new MenuItem("About Panomicon...", () ->
        Utils.showHelp(getAboutHTML(), getAboutImage())
        ));

    hm.addItem(new MenuItem("Version history...", () ->      
        Utils.showHelp(getVersionHTML(), null)
        ));
    return menuBar;
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
   */
  void addWorkflowLinks(Screen current) {
    navPanel.clear();
    for (Screen s: screens) {
      String link = s.getTitle();
      final Label label = new Label(link);
      label.setStylePrimaryName("navlink");
      if (s.additionalNavlinkStyle() != null) {
        label.addStyleDependentName(s.additionalNavlinkStyle());
      }
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
      if (screens.get(0) == s) {
        label.addStyleDependentName("first");
      }
      navPanel.add(label);
    }
  }

  /**
   * Display the screen that corresponds to a given history token.
   */
  private void showScreenForToken(String token, boolean firstLoad) {
    Screen screen;
    if (firstLoad && readURLParameters()) {
      screen = importingScreen;
    } else {
      screen = pickScreen(token);
    }
    showScreen(screen);
    Analytics.trackPageView(Analytics.URL_PREFIX + token);
  }

  /**
   * Switch screens.
   */
  protected void showScreen(Screen s) {
    if (currentScreen != null) {
      mainDockPanel.remove(currentScreen.widget());
      currentScreen.hide();
    }

    currentScreen = s;
    currentScreen.loadState(appInfo.attributes());

    menuBar.clearItems();
    List<MenuItem> allItems = new LinkedList<MenuItem>(preMenuItems);
    allItems.addAll(currentScreen.menuItems());
    allItems.addAll(postMenuItems);

    for (MenuItem mi : allItems) {
      menuBar.addItem(mi);
    }

    addWorkflowLinks(currentScreen);
    mainDockPanel.add(currentScreen.widget());
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
    if (!screensBykey.containsKey(token)) {
      return screensBykey.get(defaultScreenKey()); // default
    } else {
      return screensBykey.get(token);
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
   */
  protected void addScreenSeq(Screen s) {
    screensBykey.put(s.key(), s);    
    screens.add(s);
    s.initGUI();
  }

  /**
   * Set up the workflow sequence once.
   */
  abstract protected void initScreens();

  @Override
  public void resetWorkflowLinks() {
    addWorkflowLinks(currentScreen);
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
  
  @Override
  public NetworkServiceAsync networkService() {
    return networkService;
  }
}
