package t.common.shared.sample.search;

import java.io.Serializable;

public enum MatchType implements Serializable {
  AboveLimit,
  BelowLimit,
  Low,
  NormalRange,
  High;

  public boolean requiresValue() {
    return ((this == AboveLimit) || (this == BelowLimit));
  }
}
