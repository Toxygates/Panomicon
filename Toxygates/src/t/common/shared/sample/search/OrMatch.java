package t.common.shared.sample.search;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import t.common.shared.sample.BioParamValue;

@SuppressWarnings("serial")
public class OrMatch implements MatchCondition, Serializable {
  Collection<? extends MatchCondition> conditions;
  
  //GWT constructor
  public OrMatch() {}
  
  public OrMatch(Collection<? extends MatchCondition> conditions) {
    this.conditions = conditions;
  }
  
  @Override
  public Collection<BioParamValue> neededParameters() {
    Set<BioParamValue> r = new HashSet<BioParamValue>();
    for (MatchCondition mc: conditions) {
      r.addAll(mc.neededParameters());
    }
    return r;
  }
  
  public Collection<? extends MatchCondition> subConditions() { return conditions; }
}
