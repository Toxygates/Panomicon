package otg.model.sample;

import java.util.Collection;

import t.model.sample.Attribute;

public class AttributeSet extends t.model.sample.AttributeSet {
    public AttributeSet(Collection<Attribute> attributes, Collection<Attribute> required) {
    super(attributes, required);   
  }

  public static AttributeSet getDefault() {
    return null;
  }
}
