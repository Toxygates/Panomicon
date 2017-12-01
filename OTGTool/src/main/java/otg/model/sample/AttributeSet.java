package otg.model.sample;

import static otg.model.sample.OTGAttribute.*;
import static t.model.sample.CoreParameter.*;

import java.util.*;

import t.model.sample.Attribute;
import t.model.sample.CoreParameter;

/**
 * An AttributeSet for Open TG-GATEs data.
 */
@SuppressWarnings("serial")
public class AttributeSet extends t.model.sample.AttributeSet {
  
  //GWT constructor
  public AttributeSet() {}
  
  /**
   * Internal constructor. Users should not access this constructor directly.
   * @param attributes
   * @param required
   */
  AttributeSet(Collection<Attribute> attributes, Collection<Attribute> required) {
    super(attributes, required);
    Collections.addAll(previewDisplay, Dose, DoseUnit, DoseLevel, ExposureTime, AdmRoute, Type);
    Collections.addAll(highLevel, Type, Organism, Organ, TestType, Repeat);
    Collections.addAll(unitLevel, Compound, DoseLevel, ExposureTime);
  }
    
  private static AttributeSet defaultSet;

  /**
   * Obtain the Open TG-GATEs AttributeSet singleton.
   */
  synchronized public static AttributeSet getDefault() {
    if (defaultSet == null) {
      List<Attribute> attributes = new ArrayList<Attribute>();
      Collections.addAll(attributes, CoreParameter.values());
      Collections.addAll(attributes, otg.model.sample.OTGAttribute.values());
      List<Attribute> required = new ArrayList<Attribute>();
      
      Collections.addAll(required, SampleId, ControlGroup, Platform, Type);
      Collections.addAll(required, Organism, TestType, Repeat, Organ,
        Compound, DoseLevel, ExposureTime);
      
      defaultSet = new AttributeSet(attributes, required);
    }
    return defaultSet;
  }
  
  private List<Attribute> previewDisplay = new ArrayList<Attribute>();  
  @Override
  public Collection<Attribute> getPreviewDisplay() { return previewDisplay; }
  
  private List<Attribute> highLevel = new ArrayList<Attribute>();  
  @Override
  public Collection<Attribute> getHighLevel() { return highLevel; }
  
  private List<Attribute> unitLevel = new ArrayList<Attribute>();
  @Override
  public Collection<Attribute> getUnitLevel() { return unitLevel; }

}
