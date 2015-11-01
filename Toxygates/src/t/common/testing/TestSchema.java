/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition
 * (NIBIOHN), Japan.
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
package t.common.testing;

import t.common.shared.DataSchema;
import t.common.shared.SampleClass;

@SuppressWarnings("serial")
public class TestSchema extends DataSchema {

  public TestSchema() {
  }

  @Override
  public String[] sortedValues(String parameter) throws Exception {
    throw new Exception("Implement me");
  }

  @Override
  public String majorParameter() { return "major"; }

  @Override
  public String mediumParameter() { return "medium"; }

  @Override
  public String minorParameter() { return "minor"; }

  @Override
  public String timeParameter() { return "minor"; }

  @Override
  public String timeGroupParameter() { return "medium"; }
  
  @Override
  public String[] macroParameters() {
    return new String[] { "macro1", "macro2" };
  }

  @Override
  public String title(String parameter) {
    return "T:" + parameter;
  }

  @Override
  public int numDataPointsInSeries(SampleClass sc) {
    // TODO Auto-generated method stub
    return 0;
  }

}
