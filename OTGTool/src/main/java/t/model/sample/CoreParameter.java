package t.model.sample;

public enum CoreParameter implements Attribute {
  Batch("batchGraph", "Batch"),
  ControlGroup("control_group", "Control group"),
  DoseLevel("dose_level", "Dose level"),
  Individual("individual_id", "Individual"), 
  ExposureTime("exposure_time", "Exposure Time");
  
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
