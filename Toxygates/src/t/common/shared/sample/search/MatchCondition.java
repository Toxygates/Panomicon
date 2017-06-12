package t.common.shared.sample.search;

import java.util.Collection;

public interface MatchCondition {
  /**
   * Parameters whose values are needed to test this condition match.
   * Parameters are identified by human-readable string.
   * @return
   */
  public Collection<String> neededParameters();
}
