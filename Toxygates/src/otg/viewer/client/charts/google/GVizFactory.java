/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health
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

package otg.viewer.client.charts.google;

import java.util.List;

import otg.viewer.client.charts.*;
import otg.viewer.client.components.OTGScreen;
import t.viewer.client.storage.StorageProvider;

/**
 * Concrete factory methods that use Google Visualizations to display charts.
 */
public class GVizFactory extends Factory<GDTData, GDTDataset> {

  @Override
  public GDTData[][] dataArray(int rows, int cols) {
    return new GDTData[rows][cols];
  }

  @Override
  public GDTDataset dataset(List<ChartSample> samples, String[] categories,
      boolean categoriesAreMins, StorageProvider storage) {
    return new GDTDataset(samples, categories, categoriesAreMins, storage);
  }

  @Override
  public GVizChartGrid grid(OTGScreen screen, GDTDataset table, List<String> rowFilters,
      List<String> organisms, boolean rowsAreMajors, String[] timesOrDoses, boolean columnsAreTimes,
      int totalWidth) {
    return new GVizChartGrid(this, screen, table, rowFilters, organisms, rowsAreMajors,
        timesOrDoses, columnsAreTimes, totalWidth);
  }

  public AdjustableGrid<GDTData, GDTDataset> adjustableGrid(ChartParameters params,
      DataSource.ExpressionRowSource source) {
    return new AdjustableGrid<GDTData, GDTDataset>(this, params, source);
  }

  @Override
  public int gridMaxWidth() {
    return GVizChartGrid.MAX_WIDTH;
  }

}
