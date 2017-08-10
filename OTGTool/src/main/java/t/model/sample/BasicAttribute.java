package t.model.sample;

import javax.annotation.Nullable;

public class BasicAttribute implements Attribute {

  private String id, title;
  private @Nullable String section;
  private boolean isNumerical;
  
  BasicAttribute(String id, String title, boolean isNumerical, @Nullable String section) {
    this.id = id;
    this.title = title;
    this.isNumerical = isNumerical;
    this.section = section;
  }
  
  BasicAttribute(String id, String title, String kind, @Nullable String section) {
    this(id, title, "numerical".equals(kind), section);
  }
  
  BasicAttribute(String id, String title) {
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
  public boolean equals(Object other) {
    return Attributes.equal(this, other);
  }
  
  @Override
  public int hashCode() {
    return id.hashCode();
  }
  
  @Override
  public String toString() {
    return title;
  }

}
