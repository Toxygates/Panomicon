package t.model;

public enum SampleParameter {
  Individual("individual_id", "Individual"), 
  ExposureTime("exposure_time", "Exposure Time");
  
  SampleParameter(String id, String title) {
    this.id = id;
    this.title = title;
  }
  
  private String id;
  private String title;
  
  public String id() { return id; }
  public String title() { return title; }
}
