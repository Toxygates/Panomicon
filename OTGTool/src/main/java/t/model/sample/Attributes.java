package t.model.sample;

/**
 * Helper methods for sample attributes
 */
public class Attributes {
  
  static boolean equal(Attribute a, Object other) {
    if (other instanceof Attribute) {
      Attribute a2 = (Attribute) other;
      return a.id().equals(a2.id()); 
    }
    return false;
  }
  
}
