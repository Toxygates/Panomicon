package t.viewer.client.future;

import java.util.ArrayList;

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
  private ArrayList<Dependent> dependents = new ArrayList<Dependent>();
  
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
  
  @Override
  public Dependable addDependent(Dependent dependent) {
    dependents.add(dependent);
    dependent.startDepending(this);
    return this;
  }
  
  @Override
  public void onSuccess(T t) {
    done = true;
    result = t;
    dependents.forEach(d -> d.dependableCompleted(this));
  }

  @Override
  public void onFailure(Throwable caught) {
    done = false;
    this.caught = caught;
    dependents.forEach(d -> d.dependableCompleted(this));
  }
}
