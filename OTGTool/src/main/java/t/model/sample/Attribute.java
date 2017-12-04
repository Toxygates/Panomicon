package t.model.sample;

import javax.annotation.Nullable;

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
  
  /**
   * The section that the attribute belongs to, if any.
   * @return
   */
  public @Nullable String section();
}
