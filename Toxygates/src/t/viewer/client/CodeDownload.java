package t.viewer.client;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.user.client.Window;

abstract public class CodeDownload implements RunAsyncCallback {
  private Logger logger;

  public CodeDownload(Logger logger) {
    this.logger = logger;
  }

  public void onFailure(Throwable reason) {
    logger.log(Level.WARNING, "Code download failed", reason);
    Window.alert("Code download failed");
  }

}
