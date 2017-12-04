package t.common.shared.sample.search;

import java.io.Serializable;
import java.util.*;

import t.model.sample.Attribute;

@SuppressWarnings("serial")
public class OrMatch implements MatchCondition, Serializable {
  Collection<? extends MatchCondition> conditions;
  
  //GWT constructor
  public OrMatch() {}
  
  public OrMatch(Collection<? extends MatchCondition> conditions) {
    this.conditions = conditions;
  }
  
  @Override
  public Collection<Attribute> neededParameters() {
    Set<Attribute> r = new HashSet<Attribute>();
    for (MatchCondition mc: conditions) {
      r.addAll(mc.neededParameters());
    }
    return r;
  }
  
  public Collection<? extends MatchCondition> subConditions() { return conditions; }
}
