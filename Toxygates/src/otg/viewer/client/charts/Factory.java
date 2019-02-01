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

package otg.viewer.client.charts;

import java.util.List;

import otg.viewer.client.components.OTGScreen;
import t.viewer.client.storage.StorageProvider;

/**
 * Factory methods for constructing a family of charts.
 *
 * @param <D> the underlying Data implementation
 * @param <DS> the underlying Dataset implementation
 */
abstract public class Factory<D extends Data, DS extends Dataset<D>> {

  abstract public D[][] dataArray(int rows, int cols);

  abstract public DS dataset(List<DataPoint> samples, String[] categories,
      boolean categoriesAreMins, StorageProvider storage);

  abstract public ChartGrid<D> grid(OTGScreen screen, DS table, List<String> rowFilters,
      List<String> rowLabels, List<String> organisms, boolean rowsAreMajors, String[] timesOrDoses,
      boolean columnsAreTimes, int totalWidth);

  abstract public int gridMaxWidth();
}
