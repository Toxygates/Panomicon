package otg.model.sample;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import t.model.sample.Attribute;
import t.model.sample.CoreParameter;
import static t.model.sample.CoreParameter.*;
import static otg.model.sample.Attribute.*;

public class AttributeSet extends t.model.sample.AttributeSet {
    public AttributeSet(Collection<Attribute> attributes, Collection<Attribute> required) {
    super(attributes, required);   
    Collections.addAll(previewDisplay, Dose, DoseUnit, DoseLevel,
        ExposureTime, AdmRoute);
    Collections.addAll(highLevel, Organism, Organ, TestType, Repeat);
  }
    
  private static AttributeSet defaultSet;

  synchronized public static AttributeSet getDefault() {
    if (defaultSet == null) {
      List<Attribute> attributes = new ArrayList<Attribute>();
      Collections.addAll(attributes, CoreParameter.values());
      Collections.addAll(attributes, otg.model.sample.Attribute.values());
      //TODO
      List<Attribute> required = new ArrayList<Attribute>();
      Collections.addAll(required, CoreParameter.values());
      
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
  
}
