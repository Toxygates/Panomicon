package t.viewer.client.components;

import java.util.concurrent.CompletableFuture;

public class PACFuture<T> {
  public final PendingAsyncCallback<T> callback;
  public final CompletableFuture<T> future;
  
  public PACFuture(Screen screen, String onErrorMessage) {
    future = new CompletableFuture<T>();
    callback = new PendingAsyncCallback<T>(screen, onErrorMessage) {
      @Override
      public void handleSuccess(T result) {
        future.complete(result);
      }
      
      @Override
      public void onFailure(Throwable caught) {
        super.onFailure(caught);
        future.completeExceptionally(caught);
      }
    };
  }
}
