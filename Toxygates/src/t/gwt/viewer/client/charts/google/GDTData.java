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

package t.gwt.viewer.client.charts.google;

import com.google.gwt.visualization.client.DataTable;

import t.gwt.viewer.client.charts.Data;

/**
 * Data backed by a DataTable from the Google Visualization library.
 */
public class GDTData extends Data {

  private DataTable data;

  public GDTData(DataTable data) {
    this.data = data;
  }

  @Override
  public int numberOfColumns() {
    return data.getNumberOfColumns();
  }

  DataTable data() {
    return data;
  }
}
