package t.viewer.client.future;

import java.util.logging.Level;

import com.google.gwt.user.client.Window;

import t.viewer.client.components.Screen;

public class FutureUtils {
  private FutureUtils() {} // Prevent instantiation of this class 
  
  public interface SuccessAction<T> {
    void run(T t);
  }
  
  public static <T> void addSimpleSuccessCallback(Future<T> future, SuccessAction<T> action) {
    FutureAction futureAction = new FutureAction(() -> {
      if (future.wasSuccessful()) {
        action.run(future.result());
      }
    });
    future.addDependent(futureAction);
  }
  
  public static <T> Future<T> newPendingRequestFuture(Screen screen, String errorMessage) {
    Future<T> future = new Future<T>();
    addPendingRequestHandling(future, screen, errorMessage);
    return future;
  }
  
  public static void addPendingRequestHandling(Future<?> future, Screen screen, String errorMessage) {
    new PendingRequestHandler(future, screen, errorMessage);
  }
  
  private static class PendingRequestHandler implements Dependent {
    private Future<?> future;
    private Screen screen;
    private String errorMessage;
    
    public PendingRequestHandler(Future<?> future, Screen screen, String errorMessage) {
      this.future = future;
      this.screen = screen;
      this.errorMessage = errorMessage;
      future.addDependent(this);
    }

    @Override
    public void startDepending(Dependable dependable) {
      assert(dependable == future);
    }

    @Override
    public void dependableCompleted(Dependable dependable) {
      assert(dependable == future);
      screen.removePendingRequest();
      if (!future.wasSuccessful()) {
        screen.getLogger().log(Level.SEVERE, errorMessage, future.caught());
        Window.alert(errorMessage + ":" + future.caught().getMessage());
      }
    }
  }
}
