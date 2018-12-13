package t.viewer.client.components;

import java.util.List;
import java.util.logging.Logger;

import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.Widget;

import t.common.shared.DataSchema;
import t.model.sample.AttributeSet;
import t.viewer.client.storage.StorageProvider;
import t.viewer.shared.AppInfo;

/**
 * High-level building block for applications. A screen is a GUI with a specific
 * theme or purpose. An application consists of a series of screens.
 */
public interface Screen {
  AppInfo appInfo();
  
  DataSchema schema();
  
  AttributeSet attributes();

  Logger getLogger();

  StorageProvider getStorage();
  
  void addPendingRequest();

  void removePendingRequest();

  //Accessors
  String getTitle();
  String key();
  boolean enabled();
  Widget widget();
  List<MenuItem> menuItems();
  String additionalNavlinkStyle();

  void initGUI();

  void loadState(AttributeSet attributes);

  /**
   * The screen can potentially rebuild itself prior to being shown, by overriding this method.
   * This allows menus and the main content to change in response to saved state.
   */
  default void preShow() {}
  
  void show();

  void hide();

  void resizeInterface();

  void showGuide();

  void showHelp();
  
  void showToolbar(Widget toolbar);
  
  void hideToolbar(Widget toolbar);
}
