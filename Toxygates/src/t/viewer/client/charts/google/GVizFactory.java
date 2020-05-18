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

package t.viewer.client.charts.google;

import t.viewer.client.charts.AdjustableGrid;
import t.viewer.client.charts.ChartParameters;
import t.viewer.client.charts.DataSource;
import t.viewer.client.charts.Factory;
import t.viewer.client.components.Screen;
import t.viewer.client.charts.DataPoint;
import t.viewer.client.storage.StorageProvider;

import java.util.List;

/**
 * Concrete factory methods that use Google Visualizations to display charts.
 */
public class GVizFactory extends Factory<GDTDataset> {

  @Override
  public GDTDataset dataset(List<DataPoint> points, String[] categories,
                            boolean categoriesAreMins, StorageProvider storage) {
    return new GDTDataset(points, categories, categoriesAreMins, storage);
  }

  @Override
  public GVizChartGrid grid(Screen screen, GDTDataset table, List<String> rowFilters,
                            List<String> rowLabels,
                            List<String> organisms, boolean rowsAreMajors, String[] timesOrDoses, boolean columnsAreTimes,
                            int totalWidth) {
    return new GVizChartGrid(screen, table, rowFilters, rowLabels, organisms, rowsAreMajors,
        timesOrDoses, columnsAreTimes, totalWidth);
  }

  public AdjustableGrid<GDTDataset> adjustableGrid(ChartParameters params,
      DataSource.ExpressionRowSource source) {
    return new AdjustableGrid<GDTDataset>(this, params, source);
  }

  @Override
  public int gridMaxWidth() {
    return GVizChartGrid.MAX_WIDTH;
  }

}
