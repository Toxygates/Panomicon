package t.viewer.client;

import java.util.HashMap;
import java.util.Map;

import t.model.SampleClass;
import t.model.sample.*;

public class SampleClassPacker extends Packer<SampleClass> {
  AttributeSet attributes;

  public SampleClassPacker(AttributeSet attributes) {
    this.attributes = attributes;
  }

  @Override
  public String pack(SampleClass sampleClass) {
    StringBuilder sb = new StringBuilder();
    for (Attribute attribute : sampleClass.getKeys()) {
      sb.append(attribute.id() + ",,,");
      sb.append(sampleClass.get(attribute) + ",,,");
    }
    return sb.toString();
  }

  @Override
  public SampleClass unpack(String string) {
    String[] splits = string.split(",,,");
    Map<Attribute, String> d = new HashMap<Attribute, String>();
    for (int i = 0; i < splits.length - 1; i += 2) {
      Attribute attribute = attributes.byId(splits[i]);
      if (attribute != null) {
        d.put(attribute, splits[i + 1]);
      }
    }

    if (!d.containsKey(CoreParameter.Type)) {
      upgradeSampleClass(d);
    }

    return new SampleClass(d);
  }

  //Transitional method for upgrading from old format, as of Jan 2018
  private static void upgradeSampleClass(Map<Attribute, String> data) {
    data.put(CoreParameter.Type, "mRNA");
  }
}
