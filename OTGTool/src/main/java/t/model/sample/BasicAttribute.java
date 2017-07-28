package t.model.sample;

public class BasicAttribute implements Attribute {

  private String id, title;
  private boolean isNumerical;
  
  //TODO this contructor should not be public
  public BasicAttribute(String id, String title, boolean isNumerical) {
    this.id = id;
    this.title = title;
    this.isNumerical = isNumerical;
  }
  
  //TODO this constructor should not be public 
  public BasicAttribute(String id, String title, String kind) {
    this(id, title, kind.equals("numerical"));
  }
  
  //TODO this constructor should not be public
  public BasicAttribute(String id, String title) {
    this(id, title, false);
  }
  
  @Override
  public String id() { return id; }

  @Override
  public String title() { return title; }

  @Override
  public boolean isNumerical() { return isNumerical; }
  
  @Override
  public boolean equals(Object other) {
    return Attributes.equal(this, other);
  }
  
  @Override
  public int hashCode() {
    return id.hashCode();
  }

}
