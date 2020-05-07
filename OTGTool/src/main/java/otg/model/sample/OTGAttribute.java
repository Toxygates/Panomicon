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

import org.json.Test;
import t.model.sample.Attribute;

import java.util.Arrays;

/**
 * Attributes available in Open TG-GATEs.
 * This class defines essential attributes (needed in code) only.
 * Additional attributes are defined in the triplestore.
 */
public class OTGAttribute {
  private static final String SampleDetails = "Sample details";
  private static final String OrganWeight = "Organ weight";
  private static final String Meta = "Meta";

  public static final Attribute DoseLevel = new Attribute("dose_level", "Dose level", false, SampleDetails);
  public static final Attribute Individual = new Attribute("individual_id", "Individual", false, SampleDetails);
  public static final Attribute ExposureTime = new Attribute("exposure_time", "Exposure Time", false, SampleDetails);
  public static final Attribute Dose = new Attribute("dose", "Dose", false, SampleDetails);
  public static final Attribute DoseUnit = new Attribute("dose_unit", "Dose unit", false, SampleDetails);
  public static final Attribute Compound = new Attribute("compound_name", "Compound", false, SampleDetails);
  
  public static final Attribute Organism = new Attribute("organism", "Organism", false, SampleDetails);
  public static final Attribute Organ = new Attribute("organ_id", "Organ", false, SampleDetails);
  public static final Attribute Repeat = new Attribute("sin_rep_type", "Repeat?", false, SampleDetails);
  public static final Attribute TestType = new Attribute("test_type", "Test type", false, SampleDetails);
  
  public static final Attribute Dataset = new Attribute("dataset", "Dataset", false, Meta);

  public static final Attribute AdmRoute = new Attribute("adm_route_type", "Administration route", false, SampleDetails);
  
  public static final Attribute LiverWeight = new Attribute("liver_wt", "Liver weight (g)", true, OrganWeight);
  public static final Attribute KidneyWeight = new Attribute("kidney_total_wt", "Kidney weight total (g)", true, OrganWeight);

  private static final Attribute[] _all = { DoseLevel, Individual, ExposureTime,
          Dose, DoseUnit, Compound, Organism, Organ, Repeat, TestType, Dataset,
          AdmRoute, LiverWeight, KidneyWeight };

  public static Attribute[] all() {
    return Arrays.copyOf(_all, _all.length);
  }

  private OTGAttribute() {}
}
