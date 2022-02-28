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

package t.model.sample;

import java.util.Arrays;

/**
 * Key attributes expected from all samples in a t framework database.
 */
public class CoreParameter {
  public static final Attribute SampleId = new Attribute("sample_id", "Sample ID", false, "Sample details");
  public static final Attribute Batch = new Attribute("batchGraph", "Batch", false, "System details");
  public static final Attribute ControlGroup = new Attribute("control_group", "Control group", false, "Sample details");
  public static final Attribute Platform = new Attribute("platform_id", "Platform ID", false, "Sample details");
  public static final Attribute Type = new Attribute("type", "Type", false, "Sample details");
  public static final Attribute ControlSampleId = new Attribute("control_sample_id", "Control Sample ID", false, "Sample details");
  public static final Attribute Dataset = new Attribute("dataset", "Dataset", false, "Sample details");

  /**
   * Groups samples with the same treatment globally in the database.
   */
  public static final Attribute Treatment = new Attribute("treatment", "Treatment ID", false, "Sample details");

  /**
   * Identifies the treatment ID that corresponds to this sample's control samples.
   */
  public static final Attribute ControlTreatment = new Attribute("control_treatment", "Control Treatment", false, "Sample details");

  private static final Attribute[] _all = { SampleId, Batch, ControlGroup, Platform, Type,
          ControlSampleId, Treatment, ControlTreatment };

  public static Attribute[] all() {
    return Arrays.copyOf(_all, _all.length);
  }

  private CoreParameter() {}
}
