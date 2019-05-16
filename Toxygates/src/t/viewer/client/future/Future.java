package t.viewer.client.future;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.rpc.AsyncCallback;

import t.common.shared.Pair;

/**
 * Represents an operation that will be completed in the future, either resulting 
 * in a value of some type on successful completion, a Throwable if an error occurred.
 * A third, alternative possibility is for the future to be bypassed, representing a
 * case where the operation the future represents was bypassed instead of being executed.
 * 
 * Callbacks can be added to the future to react to the result, whether the operation
 * completed successfully, encountered an error, or was bypassed.
 * 
 * Helper methods exist to add callbacks that are only executed on successful completion
 * of the operation, or completion without an error. There is also a static method to 
 * create a future that combines the results of two futures.
 * 
 * This class also inherits from AsyncCallback, so that it can be used to represent the
 * result of RPC requests that can take an AsyncCallback.
 *  
 * @param <T> The type of value the operation will result in.
 */
public class Future<T> implements AsyncCallback<T> {
  private boolean done = false;
  private boolean bypassed = false;
  private T result;
  private Throwable caught;
  private ArrayList<Consumer<Future<T>>> callbacks = new ArrayList<Consumer<Future<T>>>();
  
  public Future() {}
  
  /**
   * Precondition: operation completed without an error or being bypassed
   * @return the result of the computation 
   */
  public T result() {
    assert(doneAndSuccessful());
    return result;
  }
  
  /**
   * Precondition: operation resulted in an error
   * @return the error encountered during the operation 
   */
  public Throwable caught() {
    assert(doneWithError());
    return caught;
  }
  
  /**
   * @return true if operation has completed (with an error or result) or has been bypassed
   */
  private boolean done() {
    return done;
  }
  
  /**
   * @return true if the operation completed without being bypassed or encountering an error
   */
  public boolean doneAndSuccessful() {
    return done && result != null;
  }
  
  /**
   * @return true if the operation completed without an error, or was bypassed
   */
  public boolean doneWithoutError() {
    return done && caught == null;
  }
  
  /**
   * @return true if the operation ran, resulting in an error
   */
  public boolean doneWithError() {
    return done && caught != null;
  }
  
  /**
   * @return true if the operation was bypassed
   */
  public boolean bypassed() {
    return done && bypassed;
  }
  
  /**
   * Adds a callback that will be called when the operation is completed (with a result
   * or an error), or bypassed.
   * @param callback the callback to be run
   * @return this object, for method chaining
   */
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
   * Adds a callback that will be called only if and when the operation completes with
   * a result, without being bypassed
   * @param callback the callback to be run
   * @return this object, for method chaining
   */
  public Future<T> addSuccessCallback(Consumer<T> callback) {
    addCallback(future -> {
      if (future.doneAndSuccessful()) {
        callback.accept(future.result());
      }
    });
    return this;
  }
  
  /**
   * Adds a callback that will be called when the operation is completed without an error,
   * i.e. either providing a result or being bypassed
   * @param callback the callback to be run
   * @return this object, for method chaining
   */
  public Future<T> addNonErrorCallback(Consumer<Future<T>> callback) {
    addCallback(future -> {
      if (future.doneWithoutError()) {
        callback.accept(future);
      }
    });
    return this;
  }
  
  /**
   * To be called to represent a situation when the operation this future represents 
   * does not actually run, but certain operations that were waiting for this future
   * to complete (those that do not require the result of the operation) should still
   * proceed.
   * @return this object, for method chaining
   */
  public Future<T> bypass() {
    bypassed = true;
    onSuccess(null);
    return this;
  }
  
  // Implementation of abstract method in AsyncCallback
  @Override
  public void onSuccess(T t) {
    done = true;
    result = t;
    callbacks.forEach(c -> c.accept(this));
  }

  // Implementation of abstract method in AsyncCallback
  @Override
  public void onFailure(Throwable caught) {
    assert(caught != null);
    done = true;
    this.caught = caught;
    callbacks.forEach(c -> c.accept(this));
  }
  
  /**
   * Combine two futures into a future that completes with a Pair containing the results
   * of the two futures.
   * If either future completes with an error then the combined future completes with the
   * same error. If either future is bypassed, then the combined future counts as having been
   * bypassed.
   * @param future1 the first future to combine
   * @param future2 the second future to combine
   * @return the combined future
   */
  public static <T,U> Future<Pair<T,U>> combine(Future<T> future1, Future<U> future2) {
    Future<Pair<T,U>> combinedFuture = new Future<Pair<T,U>>();
    
    // We use a mutable boolean-like object here to make sure that we don't call 
    // combineResults more than once.
    final AtomicBoolean combinedCallbackRan = new AtomicBoolean(false);
    
    // When future1 completes, if future 2 is done, we call combineResults to
    // generate a result for combinedFuture
    future1.addCallback(f -> {
      if (future2.done() && !combinedCallbackRan.get()) {
        combinedCallbackRan.set(true);
        combineResults(combinedFuture, future1, future2);
      }
    });
    
    // Same as above with roles reserved
    future2.addCallback(f -> {
      if (future1.done() && !combinedCallbackRan.get()) {
        combinedCallbackRan.set(true);
        combineResults(combinedFuture, future1, future2);
      }
    });
    
    return combinedFuture;
  }
  
  /**
   * Combines the results of two completed (possibly with an error, or bypassed) futures
   * and sets a combined future's result accordingly.
   * Precondition: both future1 and future2 done.
   * @param combinedFuture a future meant to combine the results of future1 and future 2
   * @param future1 a completed future
   * @param future2 another completed future
   */
  private static <T,U> void combineResults(Future<Pair<T,U>> 
      combinedFuture, Future<T> future1, Future<U> future2) {
    // if either future had an error, the combined future gets the error
    if (future1.doneWithError()) {
      combinedFuture.onFailure(future1.caught());
    } else if (future2.doneWithError()) {
      combinedFuture.onFailure(future2.caught());
    // if either future is bypassed, the combined future is bypassed
    } else if (future1.bypassed() || future2.bypassed()) {
      combinedFuture.bypass();
    // otherwise, the combined the results of the two futures
    } else {
      combinedFuture.onSuccess(new Pair<T, U>(future1.result(), future2.result()));
    }
  }
}
