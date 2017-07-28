package t.model.sample;

/**
 * An attribute of a sample
 */
public interface Attribute {

  /**
   * Internal ID for database purposes
   * @return
   */
  public String id();
  
  /**
   * Human-readable title
   * @return
   */
  public String title();
  
  /**
   * Whether the attribute is numerical
   * @return
   */
  public boolean isNumerical();
}
