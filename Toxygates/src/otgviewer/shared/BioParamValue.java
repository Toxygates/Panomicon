package otgviewer.shared;

import java.io.Serializable;

/**
 * A measured value of a biological parameter, such as the total kidney weight.
 */
@SuppressWarnings("serial")
abstract public class BioParamValue implements Serializable {

  protected String id, label;
  
  public BioParamValue() {}
  
  /**
   * @param id ID string of the parameter
   * @param label Human-readable label of the parameter
   */
  public BioParamValue(String id, String label) {
    this.id = id;
    this.label = label;
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
}
