package t.viewer.client.components.search;

import java.util.Collection;

import t.common.shared.sample.BioParamValue;
import t.common.shared.sample.search.MatchCondition;

import com.google.gwt.user.client.ui.SimplePanel;

/**
 * Visual editor for sample match conditions.
 * A condition is a conjunction of disjunctions. 
 * Conjunctions are stacked vertically, disjunctions horizontally.
 */
public class ConditionEditor extends MatchEditor {

  private SimplePanel panel = new SimplePanel();
  private AndEditor root;
  
  public ConditionEditor(Collection<BioParamValue> parameters) {
    super(null, parameters);
    clear();   
    initWidget(panel);
    panel.addStyleName("samplesearch-rootpanel");
  }
  
  /**
   * Get the current condition
   * @return
   */
  public MatchCondition getCondition() {
    return root.getCondition();
  }
  
  AndEditor newAnd() {
    return new AndEditor(this, parameters);
  }
  
  /**
   * Reset the condition.
   */
  public void clear() {
    panel.clear();
    root = newAnd();
    panel.add(root);
  }

  @Override
  void signalEdit() {
    super.signalEdit();
    conditionChanged();
  }
  
  /**
   * Invoked when the condition has changed.
   */
  public void conditionChanged() {
    
  }
}
