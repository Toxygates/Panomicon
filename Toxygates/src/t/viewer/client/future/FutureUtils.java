package t.viewer.client.future;

import java.util.logging.Level;

import com.google.gwt.user.client.Window;

import t.viewer.client.components.Screen;

/**
 * Utility methods for Futures
 */
public class FutureUtils {
  private FutureUtils() {} // Prevent instantiation of this class 
 
  /**
   * Helper method to do the following for a future:
   * 1) increment the screen's pending request counter, and decrement it when the future
   * completes.
   * 2) if the future completes with an error, display it in a popup
   * @param future the future to add a callback to
   * @param screen the screen whose pending requests counter should be modified, and in 
   * which an error message should be shown if necessary 
   * @param errorMessage the message to show before the future's throwable's message, in
   * case the future completes with an error
   * @return
   */
  public static <T> Future<T> beginPendingRequestHandling(Future<T> future, Screen screen, String errorMessage) {
    screen.addPendingRequest();
    future.addCallback(f -> {
      screen.removePendingRequest();
      if (f.doneWithError()) {
        screen.getLogger().log(Level.SEVERE, errorMessage, f.caught());
        Window.alert(errorMessage + ": " + f.caught().getMessage());
      }
    });
    return future;
  }
}
