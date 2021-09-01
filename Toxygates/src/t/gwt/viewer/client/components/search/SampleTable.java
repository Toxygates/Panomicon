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

package t.gwt.viewer.client.components.search;

import t.shared.common.sample.Sample;
import t.model.sample.Attribute;

import com.google.gwt.cell.client.TextCell;

public class SampleTable extends ResultTable<Sample> {
  private TextCell textCell = new TextCell();

  public SampleTable(Delegate delegate) {
    super(delegate);
  }

  @Override
  protected AttributeColumn<Sample> makeColumn(Attribute attribute, boolean numeric) {
    return new AttributeColumn<Sample>(textCell, attribute, numeric) {
      @Override
      public String getData(Sample sample) {
        return sample.get(attribute);
      }
    };
  }
}
