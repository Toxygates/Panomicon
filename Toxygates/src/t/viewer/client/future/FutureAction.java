package t.viewer.client.future;

import java.util.ArrayList;

public class FutureAction implements Dependent, Dependable {
  public interface CompletionAction {
    void run();
  }
  
  private CompletionAction action;
  private int dependableCount = 0;
  private int completedCount = 0;
  private ArrayList<Dependent> dependants = new ArrayList<Dependent>();
  
  public FutureAction(CompletionAction action) {
    this.action = action;
  }
  
  @Override
  public Dependable addDependent(Dependent dependant) {
    dependants.add(dependant);
    dependant.startDepending(this);
    return this;
  }
  
  @Override
  public void startDepending(Dependable dependable) {
    dependableCount++;
  }
  
  @Override
  public void dependableCompleted(Dependable dependable) {
    if (++completedCount == dependableCount) {
      action.run();
      dependants.forEach(d -> d.dependableCompleted(this));
    }
  }
}
