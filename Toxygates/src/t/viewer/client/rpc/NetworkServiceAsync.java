/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
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

import t.common.shared.ValueType;
import t.common.shared.sample.Group;
import t.viewer.shared.mirna.MirnaSource;
import t.viewer.shared.network.*;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface NetworkServiceAsync {

  void setMirnaSources(MirnaSource[] sources, AsyncCallback<Void> callback);

  void loadNetwork(List<Group> mainColumns, String[] mainProbes, List<Group> sideColumns,
      ValueType typ,      
      AsyncCallback<NetworkInfo> callback);

  void prepareNetworkDownload(Network network, Format format, String messengerWeightColumn,
      String microWeightColumn, AsyncCallback<String> callback);

}
