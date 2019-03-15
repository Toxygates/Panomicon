package t.viewer.client.future;

import java.util.ArrayList;
import java.util.function.Consumer;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Represents an operation that will be completed in the future, either resulting 
 * in a value of some type or an exception.
 * 
 * @param <T> The type of result the operation will result in.
 */
public class Future<T> implements AsyncCallback<T>, Dependable {
  private boolean done = false;
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
    return caught == null;
  }
  
  public boolean done() {
    return done;
  }
  
  public boolean doneAndSuccessful() {
    return done() && wasSuccessful();
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
  
  public Future<T> addSuccessCallback(Consumer<T> callback) {
    addCallback(future -> {
      if (future.wasSuccessful()) {
        callback.accept(future.result());
      }
    });
    return this;
  }
  
  @Override
  public Dependable addDependent(Dependent dependent) {
    dependent.onStartDepending(this);
    addCallback((future) -> {
      dependent.dependableCompleted(this);
    });
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
}
