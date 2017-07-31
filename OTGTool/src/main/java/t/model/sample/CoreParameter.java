package t.model.sample;

public enum CoreParameter implements Attribute {
  SampleId("sample_id", "Sample ID"),
  Batch("batchGraph", "Batch"),
  ControlGroup("control_group", "Control group");
  
  
  
  CoreParameter(String id, String title) {
    this.id = id;
    this.title = title;
    this.isNumerical = false;
  }
  
  private String id;
  private String title;
  private boolean isNumerical;
  
  public String id() { return id; }
  public String title() { return title; }
  public boolean isNumerical() { return isNumerical; }
}
