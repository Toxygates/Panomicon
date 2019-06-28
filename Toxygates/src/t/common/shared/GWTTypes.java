package t.common.shared;

import java.util.*;

/**
 * Builders of GWT serialization-safe types that
 * need to be reachable from shared code, not just server code.
 */
public class GWTTypes {

  public static <T> List<T> mkList() {
    return new ArrayList<T>();
  }
  
  public static <T> List<T> mkList(List<T> data) {
    return new ArrayList<T>(data);
  }
  
  public static <K,V> Map<K,V> mkMap() {
    return new HashMap<K,V>();
  }
  
}
