package otgviewer.client.components;

import java.util.List;
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
  AppInfo appInfo();

  ScreenManager manager();

  Logger getLogger();

  String getTitle();

  String key();

  void initGUI();

  void tryConfigure();

  void setConfigured(boolean cfg);

  void loadState(AttributeSet attributes);

  void loadPersistedState();

  String additionalNavlinkStyle();

  boolean enabled();

  void enqueue(QueuedAction qa);

  ClientState state();

  void addAnalysisMenuItem(MenuItem mi);

  List<MenuItem> analysisMenuItems();

  List<MenuItem> menuItems();

  void show();

  void hide();

  Widget widget();

  void resizeInterface();

  void showGuide();

  void showHelp();

  void intermineImport(List<ItemList> itemLists, List<ItemList> clusteringLists);

  void addPendingRequest();

  void removePendingRequest();

  boolean importProbes(String[] probes);

  boolean importColumns(List<Group> groups);
}
