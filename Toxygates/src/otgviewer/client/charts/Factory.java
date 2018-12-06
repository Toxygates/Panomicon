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

package otgviewer.client.charts;

import java.util.List;

import otgviewer.client.components.Screen;
import t.viewer.client.StorageParser;

abstract public class Factory<D extends Data, DS extends Dataset<D>> {

  abstract public D[][] dataArray(int rows, int cols);

  abstract public DS dataset(List<ChartSample> samples,
      String[] categories, boolean categoriesAreMins, StorageParser parser);

  abstract public ChartGrid<D> grid(Screen screen, DS table, final List<String> rowFilters,
      final List<String> organisms, boolean rowsAreMajors, String[] timesOrDoses,
      boolean columnsAreTimes, int totalWidth);

  abstract public int gridMaxWidth();
}
