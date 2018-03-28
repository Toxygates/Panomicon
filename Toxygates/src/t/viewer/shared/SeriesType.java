package t.viewer.shared;
import t.model.sample.Attribute;
import static otg.model.sample.OTGAttribute.*;

/**
 * The type of series to be ranked.
 */
public enum SeriesType {
  Time(DoseLevel, ExposureTime), Dose(ExposureTime, DoseLevel);
  
  private Attribute fixed;
  private Attribute independent;
  
  //GWT constructor
  SeriesType() {}
  
  SeriesType(Attribute fixed, Attribute independent) {
    this.fixed = fixed;
    this.independent = independent;
  }
  
  public Attribute fixedAttribute() { return fixed; }
  public Attribute independentAttribute() { return independent; }
}
