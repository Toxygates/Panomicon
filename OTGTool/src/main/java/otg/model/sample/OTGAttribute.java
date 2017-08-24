package otg.model.sample;

import static otg.model.sample.Section.OrganWeight;
import static otg.model.sample.Section.SampleDetails;

import t.model.sample.Attribute;

/**
 * Attributes available in Open TG-GATEs.
 * This enum defines essential attributes (needed in code) only.
 * Additional attributes are defined in the triplestore.
 */
public enum OTGAttribute implements Attribute {
  DoseLevel("dose_level", "Dose level", false, SampleDetails),
  Individual("individual_id", "Individual", false, SampleDetails), 
  ExposureTime("exposure_time", "Exposure Time", false, SampleDetails),
  Dose("dose", "Dose", false, SampleDetails),
  DoseUnit("dose_unit", "Dose unit", false, SampleDetails),
  Compound("compound_name", "Compound", false, SampleDetails),
  
  Organism("organism", "Organism", false, SampleDetails),
  Organ("organ_id", "Organ", false, SampleDetails),
  Repeat("sin_rep_type", "Repeat?", false, SampleDetails),
  TestType("test_type", "Test type", false, SampleDetails),
  
  AdmRoute("adm_route_type", "Administration route", false, SampleDetails),
  
  LiverWeight("liver_wt", "Liver weight (g)", true, OrganWeight),
  KidneyWeight("kidney_total_wt", "Kidney weight total (g)", true, OrganWeight);

  private String id, title;
  boolean isNumerical;
  private String section;
  
  OTGAttribute(String id, String title, boolean numerical, String section) {
    this.id = id;
    this.title = title;
    this.isNumerical = numerical;
    this.section = section;
  }
  
  OTGAttribute(String id, String title, boolean numerical, Section sec) {
    this(id, title, numerical, sec.title);
  }
  
  @Override
  public String id() { return id; }

  @Override
  public String title() { return title; }

  @Override
  public boolean isNumerical() { return isNumerical; }
  
  @Override
  public String section() { return section; }
}
