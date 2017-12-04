package t.common.shared.sample.search;

import java.util.Collection;

import t.model.sample.Attribute;

public interface MatchCondition {
  /**
   * Parameters whose values are needed to test this condition match.
   * @return
   */
  public Collection<Attribute> neededParameters();
}
