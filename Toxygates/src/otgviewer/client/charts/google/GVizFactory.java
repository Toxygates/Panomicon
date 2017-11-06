/*
 * Copyright (c) 2012-2017 Toxygates authors, National Institutes of Biomedical Innovation, Health
 * and Nutrition (NIBIOHN), Japan.
 * 
 * This file is part of Toxygates.
 * 
 * Toxygates is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * Toxygates is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Toxygates. If not,
 * see <http://www.gnu.org/licenses/>.
 */

package otgviewer.client.charts.google;

import java.util.List;
import java.util.stream.Stream;

import otgviewer.client.charts.*;
import otgviewer.client.components.Screen;
import t.common.shared.ValueType;
import t.common.shared.sample.Group;

public class GVizFactory extends Factory<GDTData, GDTDataset> {

  @Override
  public GDTData[][] dataArray(int rows, int cols) {
    return new GDTData[rows][cols];
  }

  @Override
  public GDTDataset dataset(Stream<ChartSample> samples,
      String[] categories, boolean categoriesAreMins) {
    return new GDTDataset(samples, categories, categoriesAreMins);
  }

  @Override
  public GVizChartGrid grid(Screen screen, GDTDataset table, List<String> rowFilters,
      List<String> organisms, boolean rowsAreMajors, String[] timesOrDoses,
      boolean columnsAreTimes, int totalWidth) {
    return new GVizChartGrid(this, screen, table, rowFilters, organisms, rowsAreMajors,
        timesOrDoses, columnsAreTimes, totalWidth);
  }

  public AdjustableGrid<GDTData, GDTDataset> adjustableGrid(Screen screen, DataSource source,
      List<Group> groups, ValueType vt) {
    return new AdjustableGrid<GDTData, GDTDataset>(this, screen, source, groups, vt);
  }

  @Override
  public int gridMaxWidth() {
    return GVizChartGrid.MAX_WIDTH;
  }

}
