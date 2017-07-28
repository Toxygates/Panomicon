package otg.model.sample;

public enum Attribute implements t.model.sample.Attribute {
  LiverWeight("liver_wt", "Liver weight", true),
  KidneyWeight("kidney_wt", "Kidney weight", true);

  private String id, title;
  boolean isNumerical;
  
  Attribute(String id, String title, boolean numerical) {
    this.id = id;
    this.title = title;
    this.isNumerical = numerical;
  }
  
  @Override
  public String id() { return id; }

  @Override
  public String title() { return title; }

  @Override
  public boolean isNumerical() { return isNumerical; }

}
