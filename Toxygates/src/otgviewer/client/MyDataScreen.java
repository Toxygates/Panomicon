package otgviewer.client;

import otgviewer.client.components.Screen;
import otgviewer.client.components.ScreenManager;

public class MyDataScreen extends Screen {

  public static final String key = "my";
  
  public MyDataScreen(ScreenManager man) {
    super("My data", key, false, man);
  }

}
