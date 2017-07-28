package otg.model.sample;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import t.model.sample.Attribute;
import t.model.sample.CoreParameter;

public class AttributeSet extends t.model.sample.AttributeSet {
    public AttributeSet(Collection<Attribute> attributes, Collection<Attribute> required) {
    super(attributes, required);   
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
}
