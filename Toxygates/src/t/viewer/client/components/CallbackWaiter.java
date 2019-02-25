package t.viewer.client.components;

public class CallbackWaiter {
  
  final Screen screen;
  private FinalAction finalAction;
  private int numCallbacks;
  int responseCount = 0;
  int successCount = 0;
  
  @FunctionalInterface
  public interface FinalAction {
    void run(boolean allSuccessful);
  }
  
  public CallbackWaiter(Screen screen) {
    this.screen = screen;
  }
  
  public CallbackWaiter(Screen screen, FinalAction action) {
    finalAction = action;
    this.screen = screen;
  }
  
  public void setFinalAction(FinalAction action) {
    finalAction = action;
  }
  
  protected void responseReceived(boolean success) {
    if (success) {
      successCount++;
    }
    if (++responseCount == numCallbacks) {
      finalAction.run(successCount == responseCount);
    }
  }
  
  public <T> MultiCallback<T> makeCallback(String errorMessage) {
    return new MultiCallback<T>(errorMessage);
  }

  public class MultiCallback<T> extends PendingAsyncCallback<T> {
    private T result;
    
    public MultiCallback(String errorMessage) {
      super(screen, errorMessage);
      numCallbacks++;
    }
    
    @Override
    public void handleSuccess(T t) {
      setResult(t);
      responseReceived(true);
    }
    
    private void setResult(T result) {
      this.result = result;
    }
    
    public T getResult() {
      return result;
    }
    
    @Override
    public void onFailure(Throwable caught) {
      super.onFailure(caught);
      responseReceived(false);
    }
  }
}
