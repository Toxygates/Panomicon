package t.common.shared.sample.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

@SuppressWarnings("serial")
public class AtomicMatch implements MatchCondition, Serializable {
  @Nullable Double param1;
  String paramId;
  MatchType matchType;
  
  //GWT constructor
  public AtomicMatch() {}
  
  /**
   * @param paramId parameter identified by human-readable string
   * @param matchType
   * @param param1
   */
  public AtomicMatch(String paramId, MatchType matchType,
      @Nullable Double param1) {
    this.matchType = matchType;
    this.paramId = paramId;
    this.param1 = param1;
  }
  
  @Override
  public Collection<String> neededParameters() {
    List<String> r = new ArrayList<String>();
    r.add(paramId);        
    return r;
  }
  
  public MatchType matchType() { return matchType; }
  
  public @Nullable Double param1() { return param1; }
  
  /**
   * @return parameter identified by human-readable string
   */
  public String paramId() { return paramId; }
}
