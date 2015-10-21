/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
 * (NIBIOHN), Japan.
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

import otgviewer.shared.FullMatrix;
import otgviewer.shared.Group;
import otgviewer.shared.ManagedMatrixInfo;
import otgviewer.shared.OTGSample;
import otgviewer.shared.ServerError;
import otgviewer.shared.Synthetic;
import t.common.shared.ValueType;
import t.common.shared.sample.ExpressionRow;
import t.viewer.shared.table.SortKey;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * This service obtains expression data from the 
 * underlying data store.
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
	 * @param precise If true, names must be an exact match, otherwise partial 
	 * 	name matching is used.
	 * @param titlePatternMatch If true, the query is assumed to be a partial pattern match
	 * on probe titles.
	 * @return
	 */
	@Deprecated
	public String[] identifiersToProbes(String[] identifiers, boolean precise,
			boolean titlePatternMatch);
	
	/**
	 * Convert identifiers such as genes, probe IDs and proteins into a list of probes.
	 * 
	 * TODO not clear that this should be in MatrixService
	 * 
	 * @param filter
	 * @param identifiers
	 * @param precise If true, names must be an exact match, otherwise partial 
	 * 	name matching is used.
	 * @param titlePatternMatch If true, the query is assumed to be a partial pattern match
	 * on probe titles.
	 * @param samples If null, all probes will be obtained.
	 * @return
	 */
	public String[] identifiersToProbes(String[] identifiers, boolean precise,
			boolean titlePatternMatch, List<OTGSample> samples);
	
	/**
	 * Load data into the user's session. Also perform an initial filtering. 
	 * 
	 * TODO: this call should return a FullMatrix with the first page of rows.
	 * In effect, merge loadMatrix() with the first call to matrixRows().
	 * 
	 * @param barcodes
	 * @param probes
	 * @param type
	 * @return The number of rows that remain after filtering.
	 */
	public ManagedMatrixInfo loadMatrix(List<Group> columns, 
			String[] probes, ValueType type) throws ServerError;
	
	/**
	 * Filter data that has already been loaded into the session.
	 * @param probes Probes to keep
	 * @return 
	 */
	public ManagedMatrixInfo selectProbes(String[] probes);
	
	/**
	 * Set the filtering threshold for a single column. The interpretation of the threshold
	 * depends on the column.
	 * @param column
	 * @param threshold the threshold, or null to reset.
	 * @return
	 */
	public ManagedMatrixInfo setColumnThreshold(int column, @Nullable Double threshold);
	
	/**
	 * Add a T-test/U-test/fold change difference column. Requires that loadDataset was
	 * first used to load items. After this has been done, 
	 * datasetItems or getFullData can be used as normal to obtain the data.
	 * The test is two-tailed and does not assume equal sample variances.
	 * @param g1
	 * @param g2
	 */
	public void addTwoGroupTest(Synthetic.TwoGroupSynthetic test);

	/**
	 * Remove all test columns. The result will be reflected in subsequent calls to
	 * datasetItems or getFullData.
	 */
	public void removeTwoGroupTests();
	
	/**
	 * Get one page. Requires that loadMatrix was first used to load items.
	 * @param offset
	 * @param size
	 * @param sortColumn data column to sort by (starting at 0)
	 * If this parameter is -1, the previously applied sorting is used.
	 * @param ascending Whether to use ascending sort. Applies if sortColumn is not -1.
	 * @return
	 */
	public List<ExpressionRow> matrixRows(int offset, int size, SortKey sortKey, 
			boolean ascending);
	
	/**
	 * Get all data immediately, on the level of individual values (not averaged).
	 * @param g Samples to request. If only one group is supplied, then each 
	 * individual sample will appear as a column.
	 * @param probes
	 * @param type 
	 * @param sparseRead If true, we optimise for the case of reading a 
	 * 	single probe from multiple arrays. If false, we optimise for reading full arrays. 
	 * @param withSymbols If true, gene IDs and gene symbols will also be loaded 
	 * 	into the rows (may be slightly slower)
	 * @return
	 */
	public FullMatrix getFullData(List<Group> gs, String[] probes, 
			boolean sparseRead, boolean withSymbols, ValueType type)
			throws ServerError;
	
	/**
	 * Prepare a CSV file representing the loaded data for download. 
	 * Requires that loadDataset was first used to load items.
	 * @param individualSamples if true, each individual sample is included as a 
	 * separate column (only). If false, groups are used.
	 * @return A downloadable URL.
	 */
	public String prepareCSVDownload(boolean individualSamples);

	/**
	 * Send a feedback email from a user.
	 * This should not necessarily be in MatrixService.
	 */
	public void sendFeedback(String name, String email, String feedback);

  String prepareHeatmap(List<Group> chosenColumns, String[] chosenProbes,
      ValueType valueType);
}
