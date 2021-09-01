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

package t.viewer.client.rpc;


import java.util.List;

import javax.annotation.Nullable;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import t.viewer.client.clustering.ClusteringService;
import t.viewer.shared.clustering.Algorithm;
import t.common.shared.ValueType;
import t.common.shared.sample.ExpressionRow;
import t.common.shared.sample.Group;
import t.viewer.shared.*;

/**
 * This service obtains expression data from the underlying data store.
 * 
 * @author johan
 *
 */
@RemoteServiceRelativePath("matrix")
public interface MatrixService extends ClusteringService<Group, String>, RemoteService {

  ManagedMatrixInfo loadMatrix(String id, List<Group> columns, String[] probes, ValueType type,
      List<ColumnFilter> initFilters);

  /**
   * Filter data that has already been loaded into the session.
   * 
   * @param id ID of the matrix
   * @param probes Probes to keep
   * @return
   */
  ManagedMatrixInfo selectProbes(String id, String[] probes);

  /**
   * Set the filtering threshold for a single column.
   * 
   * @param id ID of the matrix
   * @param column
   * @param filter the filter, or null to reset.
   * @return
   */
  ManagedMatrixInfo setColumnFilter(String id, int column, @Nullable ColumnFilter filter);
  
  /**
   * Clear the filters for a number of columns.
   * 
   * @param id ID of the matrix
   * @param column
   */
  ManagedMatrixInfo clearColumnFilters(String id, int[] columns);

  /**
   * Add a T-test/U-test/fold change difference/etc. column. Requires that loadDataset was first used to
   * load items. After this has been done, datasetItems or getFullData can be used as normal to
   * obtain the data. 
   * For T- and U-tests, the test is two-tailed and does not assume equal sample variances.
   * 
   * @param id ID of the matrix
   * @param g1
   * @param g2
   */
  ManagedMatrixInfo addSyntheticColumn(String id, Synthetic synth) throws ServerError;

  /**
   * Remove all synthetic columns (two-group test and static). 
   * The result will be reflected in subsequent calls to datasetItems or
   * getFullData.
   */
  ManagedMatrixInfo removeSyntheticColumns(String id) throws ServerError;

  /**
   * Get one page. Requires that loadMatrix was first used to load items.
   * 
   * @param id ID of the matrix
   * @param offset
   * @param size
   * @param sortKey data column to sort by.
   * @param ascending Whether to use ascending sort. Applies if sortColumn is not -1.
   * @return
   */
  List<ExpressionRow> matrixRows(String id, int offset, int size, SortKey sortKey, 
      boolean ascending) throws ServerError;

  /**
   * Get all data immediately, on the level of individual values (not averaged).
   * 
   * @param g Samples to request. If only one group is supplied, then each individual sample will
   *        appear as a column.
   * @param probes
   * @param type
   * @param withSymbols If true, gene IDs and gene symbols will also be loaded into the rows (may be
   *        slightly slower)
   * @return
   */
  FullMatrix getFullData(List<Group> gs, String[] probes, 
      boolean withSymbols, ValueType type) throws ServerError;

  /**
   * Prepare a CSV file representing the loaded data for download. Requires that loadDataset was
   * first used to load items.
   * 
   * @param id ID of the matrix
   * @param individualSamples if true, each individual sample is included as a separate column
   *        (only). If false, groups are used.
   * @return A downloadable URL.
   */
  String prepareCSVDownload(String id, boolean individualSamples) throws ServerError;

  /**
   * Send a feedback email from a user. This should not necessarily be in MatrixService.
   */
  void sendFeedback(String name, String email, String feedback);

  /**
   * Perform a clustering that can be used to display a heat map.
   * @param id The matrix ID. We will attempt to reuse data from this matrix if possible.
   * @param chosenColumns
   * @param chosenProbes The atomic probes (even for orthologous display) to cluster.
   * @param valueType
   * @param algorithm
   * @param featureDecimalDigits
   * @return The clusters in JSON format.
   * @throws ServerError
   */
  String prepareHeatmap(@Nullable String id, List<Group> chosenColumns, 
      @Nullable List<String> chosenProbes,
      ValueType valueType, Algorithm algorithm, int featureDecimalDigits) throws ServerError;
  
}
