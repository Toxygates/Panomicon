package t.viewer.client.future;

import java.util.logging.Level;

import com.google.gwt.user.client.Window;

import t.viewer.client.components.Screen;

public class FutureUtils {
  private FutureUtils() {} // Prevent instantiation of this class 
 
  public static void beginPendingRequestHandling(Future<?> future, Screen screen, String errorMessage) {
    screen.addPendingRequest();
    future.addCallback(f -> {
      screen.removePendingRequest();
      if (!f.wasSuccessful()) {
        screen.getLogger().log(Level.SEVERE, errorMessage, f.caught());
        Window.alert(errorMessage + ":" + f.caught().getMessage());
      }
    });
  }
}
