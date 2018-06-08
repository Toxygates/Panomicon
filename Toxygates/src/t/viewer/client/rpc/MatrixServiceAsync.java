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

package t.viewer.client.rpc;

import java.util.List;

import t.clustering.client.ClusteringServiceAsync;
import t.clustering.shared.Algorithm;
import t.common.shared.ValueType;
import t.common.shared.sample.ExpressionRow;
import t.common.shared.sample.Group;
import t.viewer.shared.*;
import t.viewer.shared.network.Format;
import t.viewer.shared.network.Network;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface MatrixServiceAsync extends ClusteringServiceAsync<Group,String> {

  void loadMatrix(String id, List<Group> columns, String[] probes, ValueType type,
      List<ColumnFilter> initFilters,
      List<Synthetic> initSynthetics, AsyncCallback<ManagedMatrixInfo> callback);

  void matrixRows(String id, int offset, int size, SortKey sortKey, boolean ascending,
      AsyncCallback<List<ExpressionRow>> callback);

  void selectProbes(String id, String[] probes, AsyncCallback<ManagedMatrixInfo> callback);

  void setColumnFilter(String id, int column, ColumnFilter filter,
      AsyncCallback<ManagedMatrixInfo> callback);

  void getFullData(List<Group> g, String[] probes, boolean withSymbols,
      ValueType typ, AsyncCallback<FullMatrix> callback);

  void prepareCSVDownload(String id, boolean individualSamples, AsyncCallback<String> callback);

  void addSyntheticColumn(String id, Synthetic synth, AsyncCallback<ManagedMatrixInfo> callback);
  
  void removeSyntheticColumns(String id, AsyncCallback<ManagedMatrixInfo> callback);

  void sendFeedback(String name, String email, String feedback, AsyncCallback<Void> callback);

  void prepareHeatmap(String id, List<Group> chosenColumns, List<String> chosenProbes, ValueType valueType,
      Algorithm algorithm, int featureDecimalDigits, AsyncCallback<String> callback);

  void prepareNetworkDownload(Network network, Format format, AsyncCallback<String> callback);

}
