package t.common.shared.sample.search;

import java.util.Collection;

import t.common.shared.sample.BioParamValue;

public interface MatchCondition {
  /**
   * Parameters whose values are needed to test this condition match.
   * @return
   */
  public Collection<BioParamValue> neededParameters();
}
