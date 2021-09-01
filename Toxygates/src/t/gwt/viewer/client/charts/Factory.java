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

package t.gwt.viewer.client.charts;

import t.gwt.viewer.client.screen.Screen;
import t.gwt.viewer.client.storage.StorageProvider;

import java.util.List;

/**
 * Factory methods for constructing a family of charts.
 *
 * @param <D> the underlying Data implementation
 * @param <DS> the underlying Dataset implementation
 */
abstract public class Factory<DS extends Dataset<?>> {

  /**
   * Construct a new dataset that can back a number of charts
   * @return
   */
  abstract public DS dataset(List<DataPoint> samples, String[] categories,
                             boolean categoriesAreMins, StorageProvider storage);

  /**
   * Construct a new grid of individual charts.
   * 
   * @return
   */
  abstract public ChartGrid<?> grid(Screen screen, DS table, List<String> rowFilters,
                                    List<String> rowLabels, List<String> organisms, boolean rowsAreMajors, String[] timesOrDoses,
                                    boolean columnsAreTimes, int totalWidth);

  abstract public int gridMaxWidth();
}
