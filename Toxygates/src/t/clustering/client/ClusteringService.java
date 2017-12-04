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


package t.clustering.client;

import java.util.List;

import t.clustering.shared.Algorithm;

import com.google.gwt.user.client.rpc.RemoteService;

/**
 * A clustering service to generate heatmaps.
 *
 * @param <C> Identifies columns.
 * @param <R> Identifies rows.
 */
public interface ClusteringService<C, R> extends RemoteService {

  
  /**
   * Perform a clustering for the named columns and rows, and return the result as a 
   * JSON String.
   * @param columns
   * @param rows
   * @param algorithm
   * @return
   */
  String prepareHeatmap(List<C> columns, List<R> rows, Algorithm algorithm,
      int featureDecimalDigits);
}
