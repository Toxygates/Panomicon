package t.viewer.client.components;

import java.util.logging.Logger;

import t.common.shared.DataSchema;
import t.model.sample.AttributeSet;
import t.viewer.shared.AppInfo;

/**
 * Transitional interface with core features from otg.viewer.components.Screen.
 * May eventually be made permanent, or retired if Screen is moved into this package.
 */
public interface ViewContext {
  AppInfo appInfo();
  
  DataSchema schema();
  
  AttributeSet attributes();

  Logger getLogger();
  
  void addPendingRequest();

  void removePendingRequest(); 
}
