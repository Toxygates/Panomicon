package t.viewer.client.components;

import java.util.logging.Logger;

import t.common.shared.DataSchema;
import t.model.sample.AttributeSet;
import t.viewer.shared.AppInfo;

public interface ViewContext {
  AppInfo appInfo();
  
  DataSchema schema();
  
  AttributeSet attributes();

  Logger getLogger();
  
  void addPendingRequest();

  void removePendingRequest(); 
}
