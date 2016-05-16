/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health
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

package t.viewer.client.rpc;


import java.util.List;

import javax.annotation.Nullable;

import t.common.shared.ValueType;
import t.common.shared.sample.ExpressionRow;
import t.common.shared.sample.Group;
import t.common.shared.sample.Sample;
import t.common.shared.userclustering.Algorithm;
import t.viewer.shared.ColumnFilter;
import t.viewer.shared.FullMatrix;
import t.viewer.shared.ManagedMatrixInfo;
import t.viewer.shared.ServerError;
import t.viewer.shared.Synthetic;
import t.viewer.shared.table.SortKey;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * This service obtains expression data from the underlying data store.
 * 
 * @author johan
 *
 */
@RemoteServiceRelativePath("matrix")
public interface MatrixService extends RemoteService {


  /**
   * Convert identifiers such as genes, probe IDs and proteins into a list of probes.
   * 
   * TODO not clear that this should be in MatrixService
   * 
   * @param filter
   * @param identifiers
   * @param precise If true, names must be an exact match, otherwise partial name matching is used.
   * @param titlePatternMatch If true, the query is assumed to be a partial pattern match on probe
   *        titles.
   * @param samples If null, all probes will be obtained.
   * @return
   */
  public String[] identifiersToProbes(String[] identifiers, boolean precise,
      boolean titlePatternMatch, @Nullable List<Sample> samples);

  /**
   * Load data into the user's session. Also perform an initial filtering.
   * 
   * TODO: this call should return a FullMatrix with the first page of rows. In effect, merge
   * loadMatrix() with the first call to matrixRows().
   * 
   * @param barcodes
   * @param probes
   * @param type
   * @return The number of rows that remain after filtering.
   */
  public ManagedMatrixInfo loadMatrix(List<Group> columns, String[] probes, ValueType type)
      throws ServerError;

  /**
   * Filter data that has already been loaded into the session.
   * 
   * @param probes Probes to keep
   * @return
   */
  public ManagedMatrixInfo selectProbes(String[] probes);

  /**
   * Set the filtering threshold for a single column.
   * 
   * @param column
   * @param filter the filter, or null to reset.
   * @return
   */
  public ManagedMatrixInfo setColumnFilter(int column, @Nullable ColumnFilter filter);

  /**
   * Add a T-test/U-test/fold change difference column. Requires that loadDataset was first used to
   * load items. After this has been done, datasetItems or getFullData can be used as normal to
   * obtain the data. The test is two-tailed and does not assume equal sample variances.
   * 
   * @param g1
   * @param g2
   */
  public ManagedMatrixInfo addTwoGroupTest(Synthetic.TwoGroupSynthetic test) throws ServerError;

  /**
   * Remove all test columns. The result will be reflected in subsequent calls to datasetItems or
   * getFullData.
   */
  public ManagedMatrixInfo removeTwoGroupTests() throws ServerError;

  /**
   * Get one page. Requires that loadMatrix was first used to load items.
   * 
   * @param offset
   * @param size
   * @param sortColumn data column to sort by (starting at 0) If this parameter is -1, the
   *        previously applied sorting is used.
   * @param ascending Whether to use ascending sort. Applies if sortColumn is not -1.
   * @return
   */
  public List<ExpressionRow> matrixRows(int offset, int size, SortKey sortKey, boolean ascending)
      throws ServerError;

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
  public FullMatrix getFullData(List<Group> gs, String[] probes, 
      boolean withSymbols, ValueType type) throws ServerError;

  /**
   * Prepare a CSV file representing the loaded data for download. Requires that loadDataset was
   * first used to load items.
   * 
   * @param individualSamples if true, each individual sample is included as a separate column
   *        (only). If false, groups are used.
   * @return A downloadable URL.
   */
  public String prepareCSVDownload(boolean individualSamples) throws ServerError;

  /**
   * Send a feedback email from a user. This should not necessarily be in MatrixService.
   */
  public void sendFeedback(String name, String email, String feedback);

  public String prepareHeatmap(List<Group> chosenColumns, String[] chosenProbes,
      ValueType valueType, Algorithm algorithm) throws ServerError;
}
