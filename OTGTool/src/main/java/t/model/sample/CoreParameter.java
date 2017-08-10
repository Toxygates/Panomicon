package t.model.sample;

/**
 * Key attributes expected from all samples in a t framework database.
 */
public enum CoreParameter implements Attribute {
  SampleId("sample_id", "Sample ID", "Sample details"),
  Batch("batchGraph", "Batch", "System details"),
  ControlGroup("control_group", "Control group", "Sample details"),
  Platform("platform_id", "Platform ID", "Sample details");
  
  CoreParameter(String id, String title, String section) {
    this.id = id;
    this.title = title;
    this.isNumerical = false;
    this.section = section;
  }
  
  private String id;
  private String title;
  private boolean isNumerical;
  private String section;
  
  public String id() { return id; }
  public String title() { return title; }
  public boolean isNumerical() { return isNumerical; }
  public String section() { return section; }
}
