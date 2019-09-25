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

/**
 * Key attributes expected from all samples in a t framework database.
 */
public enum CoreParameter implements Attribute {
  SampleId("sample_id", "Sample ID", "Sample details"),
  Batch("batchGraph", "Batch", "System details"),
  ControlGroup("control_group", "Control group", "Sample details"),
  Platform("platform_id", "Platform ID", "Sample details"),
  Type("type", "Type", "Sample details"),
  ControlSampleId("control_sample_id", "Control Sample ID", "Sample details");
  
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
  
  @Override
  public String id() { return id; }
  @Override
  public String title() { return title; }
  @Override
  public boolean isNumerical() { return isNumerical; }
  @Override
  public String section() { return section; }
  
}
