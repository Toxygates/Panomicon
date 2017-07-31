package otg.model.sample;

public enum Attribute implements t.model.sample.Attribute {
  DoseLevel("dose_level", "Dose level"),
  Individual("individual_id", "Individual"), 
  ExposureTime("exposure_time", "Exposure Time"),
  Dose("dose", "Dose"),
  DoseUnit("dose_unit", "Dose unit"),
  
  Organism("organism", "Organism"),
  Organ("organ_id", "Organ"),
  Repeat("sin_rep_type", "Repeat?"),
  TestType("test_type", "Test type"),
  
  AdmRoute("adm_route_type", "Administration route"),
  
  LiverWeight("liver_wt", "Liver weight", true),
  KidneyWeight("kidney_wt", "Kidney weight", true);

  private String id, title;
  boolean isNumerical;
  
  Attribute(String id, String title, boolean numerical) {
    this.id = id;
    this.title = title;
    this.isNumerical = numerical;
  }
  
  Attribute(String id, String title) {
    this(id, title, false);
  }
  
  @Override
  public String id() { return id; }

  @Override
  public String title() { return title; }

  @Override
  public boolean isNumerical() { return isNumerical; }

}
