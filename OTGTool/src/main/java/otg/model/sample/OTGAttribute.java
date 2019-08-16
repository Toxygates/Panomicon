/*
 * Copyright (c) 2012-2019 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition (NIBIOHN), Japan.
 *
 * This file is part of Toxygates.
 *
 * Toxygates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Toxygates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Toxygates. If not, see <http://www.gnu.org/licenses/>.
 */

package otg.model.sample;

import static otg.model.sample.Section.*;

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
  
  Dataset("dataset", "Dataset", false, Meta),

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
