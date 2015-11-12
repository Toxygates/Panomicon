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
import otgviewer.shared.Synthetic;
import t.common.shared.ValueType;
import t.common.shared.sample.ExpressionRow;
import t.common.shared.userclustering.Algorithm;
import t.viewer.shared.table.SortKey;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface MatrixServiceAsync {
	
	public void loadMatrix(List<Group> columns,
			String[] probes, ValueType type, AsyncCallback<ManagedMatrixInfo> callback);

	public void matrixRows(int offset, int size, SortKey sortKey,
			boolean ascending, AsyncCallback<List<ExpressionRow>> callback);

	public void identifiersToProbes(String[] identifiers,
			boolean precise, boolean titlePatternMatch,
			@Nullable List<OTGSample> samples, AsyncCallback<String[]> callback);

	public void selectProbes(String[] probes,
			AsyncCallback<ManagedMatrixInfo> callback);

	public void setColumnThreshold(int column, @Nullable Double threshold,
			AsyncCallback<ManagedMatrixInfo> callback);

	public void getFullData(List<Group> g, String[] probes,
			boolean sparseRead, boolean withSymbols, ValueType typ,
			AsyncCallback<FullMatrix> callback);

	public void prepareCSVDownload(boolean individualSamples,
			AsyncCallback<String> callback);

	public void addTwoGroupTest(Synthetic.TwoGroupSynthetic test,
			AsyncCallback<Void> callback);
	
	public void removeTwoGroupTests(AsyncCallback<Void> callback);

	public void sendFeedback(String name, String email, String feedback, 
			AsyncCallback<Void> callback);

  public void prepareHeatmap(List<Group> chosenColumns, String[] chosenProbes,
      ValueType valueType, Algorithm algorithm, AsyncCallback<String> prepareHeatmapCallback);
	
}
