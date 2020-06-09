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

package t.viewer.client.charts;

import t.common.shared.DataSchema;
import t.common.shared.SharedUtils;
import t.common.shared.sample.Sample;
import t.model.SampleClass;

import javax.annotation.Nullable;
import java.util.List;
import java.util.logging.Logger;

/**
 * A Dataset can construct Data objects based on SampleClass filters. Each Data object can support
 * one chart.
 */
abstract public class Dataset<D extends Data> {

  protected List<DataPoint> points;
  protected String[] categories;
  protected boolean categoriesAreMins;

  protected double min = Double.NaN;
  protected double max = Double.NaN;

  protected Logger logger = SharedUtils.getLogger("ChartDataset");

  protected Dataset(List<DataPoint> points, String[] categories,
      boolean categoriesAreMins) {
    this.points = points;
    this.categoriesAreMins = categoriesAreMins;
    this.categories = categories;
    init();
  }

  protected void init() {
    for (DataPoint s : points) {
      if (s.value < min || Double.isNaN(min)) {
        min = s.value;
      }
      if (s.value > max || Double.isNaN(max)) {
        max = s.value;
      }
    }
  }

  /**
   * Minimum value across the whole sample space.
   */
  public double getMin() {
    return min;
  }

  /**
   * Maximum value across the whole sample space.
   */
  public double getMax() {
    return max;
  }

  /**
   * Get the sample corresponding to a particular row and column. May be null.
   */
  Sample getSample(int row, int column) {
    return null;
  }

  /**
   * Make a table corresponding to the given filter and (optionally) probe. If a time is given, the
   * table will be grouped by dose. If a dose is given, the table will be grouped by time.
   * 
   * @param filter Sample filter
   */
  abstract public D makeData(SampleClass filter, @Nullable String probe);

  protected String categoryForPoint(DataPoint sample) {
    DataSchema schema = sample.schema();
    for (String category : categories) {
      if (categoriesAreMins && schema.getMinor(sample).equals(category)) {
        return category;
      } else if (!categoriesAreMins && schema.getMedium(sample).equals(category)) {
        return category;
      }
    }
    return null;
  }

  abstract protected void makeColumns(D dt, DataPoint[] samples);

  abstract protected D[][] makeDataArray(int rows, int cols);

}
