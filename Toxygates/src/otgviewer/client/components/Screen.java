package otgviewer.client.components;

import java.util.*;
import java.util.logging.Logger;

import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.Widget;

import otgviewer.client.components.DLWScreen.QueuedAction;
import t.common.shared.ItemList;
import t.common.shared.sample.Group;
import t.model.sample.AttributeSet;
import t.viewer.client.ClientState;
import t.viewer.shared.AppInfo;

/**
 * Minimal screen interface encapsulating the core functionality in DLWScreen that will need to be
 * implemented by new screens not deriving from DataListenerWidget.
 */
public interface Screen {
  default AppInfo appInfo() {
    return manager().appInfo();
  }

  // Accessors
  ScreenManager manager();
  Logger getLogger();
  String getTitle();
  String key();
  boolean enabled();
  Widget widget();
  List<MenuItem> analysisMenuItems();
  List<MenuItem> menuItems();
  ClientState state();
  String additionalNavlinkStyle();

  void initGUI();

  void tryConfigure();

  void setConfigured(boolean cfg);

  void loadState(AttributeSet attributes);

  void loadPersistedState();

  void enqueue(QueuedAction qa);

  void addAnalysisMenuItem(MenuItem mi);

  void show();

  void hide();

  void resizeInterface();

  void showGuide();

  void showHelp();

  void intermineImport(List<ItemList> itemLists, List<ItemList> clusteringLists);

  void addPendingRequest();

  void removePendingRequest();

  boolean importProbes(String[] probes);

  boolean importColumns(List<Group> groups);
  
  /**
   * This screen may optionally replace itself with another one when being shown,
   * based on the current stored state. Such a replacement screen should be returned here.
   * If no replacement is desired, the screen should return itself.
   */
  default Screen preferredReplacement() {
    return this;
  }
  
  /**
   * A collection of all potential replacements for this screen. (See above)
   */
  default Collection<Screen> potentialReplacements() {
    return new ArrayList<Screen>();
  }
}
