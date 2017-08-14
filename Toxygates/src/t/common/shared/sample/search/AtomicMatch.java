package t.common.shared.sample.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import t.model.sample.Attribute;

@SuppressWarnings("serial")
public class AtomicMatch implements MatchCondition, Serializable {
  @Nullable Double param1;
  Attribute attr;
  MatchType matchType;
  
  //GWT constructor
  public AtomicMatch() {}
  
  /**
   * @param paramId parameter (the value of the parameter is irrelevant)
   * @param matchType
   * @param param1
   */
  public AtomicMatch(Attribute paramId, MatchType matchType,
      @Nullable Double param1) {
    this.matchType = matchType;
    this.attr = paramId;
    this.param1 = param1;
  }
  
  @Override
  public Collection<Attribute> neededParameters() {
    List<Attribute> r = new ArrayList<Attribute>();
    r.add(attr);        
    return r;
  }
  
  public MatchType matchType() { return matchType; }
  
  public @Nullable Double param1() { return param1; }
  
  /**
   * @return parameter identified by human-readable string
   */
  public Attribute parameter() { return attr; }
}
