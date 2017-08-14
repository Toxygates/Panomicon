package t.model.sample;

import java.util.Comparator;

public class AttributeComparator implements Comparator<Attribute> {

  @Override
  public int compare(Attribute o1, Attribute o2) {
    return o1.title().compareTo(o2.title());
  }

}
