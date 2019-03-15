package t.viewer.client.future;

import java.util.ArrayList;

public class FutureAction implements Dependent, Dependable {
  public interface CompletionAction {
    void run();
  }
  
  private CompletionAction action;
  private int dependableCount = 0;
  private int completedCount = 0;
  private ArrayList<Dependent> dependents = new ArrayList<Dependent>();
  
  public FutureAction() {}
  
  public FutureAction(CompletionAction action) {
    this.action = action;
  }
  
  public FutureAction setCompletionAction(CompletionAction action) {
    this.action = action;
    return this;
  }
  
  @Override
  public Dependable addDependent(Dependent dependant) {
    dependents.add(dependant);
    dependant.onStartDepending(this);
    return this;
  }
  
  @Override
  public FutureAction dependOn(Dependable dependable) {
    dependable.addDependent(this);
    return this;
  }
  
  @Override
  public void onStartDepending(Dependable dependable) {
    dependableCount++;
  }
  
  @Override
  public void dependableCompleted(Dependable dependable) {
    if (++completedCount == dependableCount) {
      action.run();
      dependents.forEach(d -> d.dependableCompleted(this));
    }
  }
}
