package t.model.sample;

import java.io.Serializable;

import javax.annotation.Nullable;

/**
 * A generic attribute. In most cases, users should not create attributes directly,
 * but request them through an attribute set, to ensure that no duplicates exist.
 */
@SuppressWarnings("serial")
public class BasicAttribute implements Attribute, Serializable {

  private String id, title;
  private @Nullable String section;
  private boolean isNumerical;
  
  //GWT constructor
  public BasicAttribute() {}
  
  public BasicAttribute(String id, String title, boolean isNumerical, @Nullable String section) {
    this.id = id;
    this.title = title;
    this.isNumerical = isNumerical;
    this.section = section;
  }
  
  public BasicAttribute(String id, String title, String kind, @Nullable String section) {
    this(id, title, "numerical".equals(kind), section);
  }
  
  public BasicAttribute(String id, String title) {
    this(id, title, false, null);
  }
  
  @Override
  public String id() { return id; }

  @Override
  public String title() { return title; }

  @Override
  public boolean isNumerical() { return isNumerical; }
  
  @Override
  public @Nullable String section() { return section; }
  
  @Override
  public String toString() {
    return title;
  }

  @Override
  public int hashCode() {
    return AttributeSet.attributeHash(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Attribute) {
      return AttributeSet.attributesEqual(this, (Attribute) obj);
    } else {
      return false;
    }
  }
  
  
}
