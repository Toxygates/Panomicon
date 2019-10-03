package t.viewer.client.components;

import com.google.gwt.user.client.Window;

/**
 * Class for managing alerts that should not be repeated excessively. 
 */
public class NonRepeatingAlert {
  private String alertText;
  private boolean hasRun = false;
  
  public NonRepeatingAlert(String alertText) {
    this.alertText = alertText;
  }
  
  /**
   * Show the alert, unless it has run already, or if force = true.
   */
  public void possiblyAlert(boolean force) {
    if (force || !hasRun) {
      hasRun = true;
      Window.alert(alertText);
    }
  }

  /**
   * Reset the alert, so that it'll be shown next time possiblyAlert is called.
   */
  public void reset() {
    hasRun = false;
  }
}
