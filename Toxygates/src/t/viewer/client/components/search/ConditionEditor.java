package t.viewer.client.components.search;

import java.util.Collection;

import t.common.shared.sample.search.AndMatch;
import t.common.shared.sample.search.MatchCondition;

/**
 * Visual editor for sample match conditions.
 * A condition is a conjunction of disjunctions. 
 * Conjunctions are stacked vertically, disjunctions horizontally.
 */
public class ConditionEditor extends MatchEditor {

  private AndEditor root;
  
  public ConditionEditor(Collection<String> parameters) {
    super(null, parameters);
    root = newAnd();
    initWidget(root);
  }
  
  /**
   * Get the current condition
   * @return
   */
  public MatchCondition getCondition() {
    return root.getCondition();
  }
  
  /**
   * Set the editor's current condition
   * @param cond
   */
  public void setCondition(AndMatch cond) {
    
  }
  
  AndEditor newAnd() {
    return new AndEditor(this, parameters);
  }

}
