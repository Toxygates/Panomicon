package t.viewer.client.future;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.rpc.AsyncCallback;

import t.common.shared.Pair;

/**
 * Represents an operation that will be completed in the future, either resulting 
 * in a value of some type or an exception.
 * 
 * @param <T> The type of result the operation will result in.
 */
public class Future<T> implements AsyncCallback<T> {
  private boolean done = false;
  private boolean bypassed = false;
  private T result;
  private Throwable caught;
  private ArrayList<Consumer<Future<T>>> callbacks = new ArrayList<Consumer<Future<T>>>();
  
  public Future() {}
  
  public T result() {
    assert(done);
    return result;
  }
  
  public Throwable caught() {
    assert(done);
    return caught;
  }
  
  public boolean wasSuccessful() {
    assert(done);
    return !bypassed && caught == null;
  }
  
  public boolean done() {
    return done;
  }
  
  public boolean doneAndSuccessful() {
    return done() && wasSuccessful();
  }
  
  public boolean doneWithoutError() {
    return done() && caught == null;
  }
  
  public boolean doneWithError() {
    return done() && caught != null;
  }
  
  public boolean actuallyRan() {
    return done && !bypassed;
  }
  
  public boolean bypassed() {
    return done && bypassed;
  }
  
  public Future<T> addCallback(Consumer<Future<T>> callback) {
    if (!done) {
      callbacks.add(callback);
    } else {
      Scheduler.get().scheduleDeferred(() -> {
        callback.accept(this);
      });
    }
    return this;
  }
  
  /**
   * 
   * @param callback
   * @return
   */
  public Future<T> addSuccessCallback(Consumer<T> callback) {
    addCallback(future -> {
      if (future.wasSuccessful()) {
        callback.accept(future.result());
      }
    });
    return this;
  }
  
  public Future<T> addNonErrorCallback(Consumer<Future<T>> callback) {
    addCallback(future -> {
      if (future.done() && future.caught() == null) {
        callback.accept(future);
      }
    });
    return this;
  }
  
  
  public Future<T> bypass() {
    bypassed = true;
    onSuccess(null);
    return this;
  }
  
  @Override
  public void onSuccess(T t) {
    done = true;
    result = t;
    callbacks.forEach(c -> c.accept(this));
  }

  @Override
  public void onFailure(Throwable caught) {
    done = true;
    this.caught = caught;
    callbacks.forEach(c -> c.accept(this));
  }
  
  public static <T,U> Future<Pair<T,U>> combine(Future<T> future1, Future<U> future2) {
    Future<Pair<T,U>> combinedFuture = new Future<Pair<T,U>>();
    
    final AtomicBoolean combinedCallbackRan = new AtomicBoolean(false);
    
    future1.addCallback(f -> {
      if (future2.done() && !combinedCallbackRan.get()) {
        combinedCallbackRan.set(true);
        combineResults(combinedFuture, future1, future2);
      }
    });
    
    future2.addCallback(f -> {
      if (future1.done() && !combinedCallbackRan.get()) {
        combinedCallbackRan.set(true);
        combineResults(combinedFuture, future1, future2);
      }
    });
    
    return combinedFuture;
  }
  
  /**
   * Precondition: both future1 and future2 done.
   */
  private static <T,U> void combineResults(Future<Pair<T,U>> 
      combinedFuture, Future<T> future1, Future<U> future2) {
    if (future1.doneWithError()) {
      combinedFuture.onFailure(future1.caught());
    } else if (future2.doneWithError()) {
      combinedFuture.onFailure(future2.caught());
    } else if (future1.bypassed() || future2.bypassed()) {
      combinedFuture.bypass();
    } else {
      combinedFuture.onSuccess(new Pair<T, U>(future1.result(), future2.result()));
    }
  }
}
