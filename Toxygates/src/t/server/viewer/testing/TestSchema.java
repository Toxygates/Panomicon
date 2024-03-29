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
package t.server.viewer.testing;

import t.model.sample.OTGAttribute;
import t.shared.common.DataSchema;
import t.shared.common.SeriesType;
import t.model.SampleClass;
import t.model.sample.Attribute;

@SuppressWarnings("serial")
public class TestSchema extends DataSchema {

  public TestSchema() {
  }

  @Override
  public String[] sortedValues(Attribute parameter) throws Exception {
    throw new Exception("Implement me");
  }

  @Override
  public Attribute majorParameter() {
    return OTGAttribute.Compound;
  }

  @Override
  public Attribute mediumParameter() {
    return OTGAttribute.DoseLevel;
  }

  @Override
  public Attribute minorParameter() {
    return OTGAttribute.ExposureTime;
  }

  @Override
  public Attribute timeParameter() {
    return OTGAttribute.ExposureTime;
  }

  @Override
  public Attribute timeGroupParameter() {
    return OTGAttribute.DoseLevel;
  }
  
  @Override
  public Attribute[] macroParameters() {
    return new Attribute[] {OTGAttribute.Organism, OTGAttribute.TestType,
        OTGAttribute.Organ, OTGAttribute.Repeat};
  }

  @Override
  public int numDataPointsInSeries(SampleClass sc, SeriesType st) {
    return 0;
  }

}
