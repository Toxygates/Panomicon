package t.common.shared.sample;

import java.io.Serializable;

import javax.annotation.Nullable;

/**
 * A measured value of a biological parameter, such as the total kidney weight.
 */
@SuppressWarnings("serial")
abstract public class BioParamValue implements Serializable, Comparable<BioParamValue> {

  protected String id, label, section;
  
  public BioParamValue() {}
  
  /**
   * @param id ID string of the parameter
   * @param label Human-readable label of the parameter
   */
  public BioParamValue(String id, String label, @Nullable String section) {
    this.id = id;
    this.label = label;
    this.section = section;
  }
  
  public String label() { return label; }
  public String id() { return id; }
  
  /**
   * Human-readable observed value.
   * @return
   */
  abstract public String displayValue();
  
  public String tooltip() { return displayValue(); } 

  public boolean isPathological() { return false; }
  
  public @Nullable String section() { return section; }
  
  public int compareTo(BioParamValue other) {
    return label().compareTo(other.label());
  }
}
