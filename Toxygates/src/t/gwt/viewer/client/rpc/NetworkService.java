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

package t.gwt.viewer.client.rpc;

import java.util.List;

import t.shared.common.ValueType;
import t.shared.common.sample.Group;
import t.shared.viewer.TimeoutException;
import t.shared.viewer.mirna.MirnaSource;
import t.shared.viewer.network.*;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("network")
public interface NetworkService extends RemoteService {

  /**
   * Prefix for matrix IDs in network state. To be prepended by client.
   */
  String tablePrefix = "network.";
  
  /**
   * Set the desired miRNA association sources. In the objects passed in,
   * only the ID string and the score threshold (lower bound) are used.
   * The choices are persisted in the user's server side session.
   * @param sources
   * @throws TimeoutException
   */
  void setMirnaSources(MirnaSource[] sources) throws TimeoutException;
  
  /**
   * Load a network.
   * @param mainId ID of the main table. Will be persisted and accessible via the
   * MatrixService.
   * @param mainColumns Columns for the main table
   * @param mainProbes Probes to load in the main table (or empty for default set,
   * follows the same rules as MatrixService.loadMatrix)
   * @param sideId ID of the side table. Will be persisted and accessible via the
   * MatrixService.  
   * @param sideColumns Columns for the side table. Probes will be determined by 
   *    associations from the main table.
   * @return Information about the loaded network.
   */
  NetworkInfo loadNetwork(String mainId, List<Group> mainColumns, 
                          String[] mainProbes,
                          String sideId, List<Group> sideColumns, 
                          ValueType typ);

  /**
   * Obtain a view of the current network, identified by the id of the main table matrix,
   * that respects sorting and filtering. A size cutoff may be applied.
   * @param mainId
   * @return
   */
  Network currentView(String mainId);

  /**
   * Serialize an interaction network to a downloadable file. 
   * @param network the network to serialize.
   * @param format the format to use for serialization.
   * @return a downloadable URL.
   */
  String prepareNetworkDownload(Network network, Format format, 
      String messengerWeightColumn, String microWeightColumn);

  /**
   * Serialize an interaction network to a downloadable file. 
   * @param mainTableId the ID of the main table that contains the network.
   * @param format the format to use for serialization.
   * @return a downloadable URL.
   */
  String prepareNetworkDownload(String mainTableId, Format format, 
      String messengerWeightColumn, String microWeightColumn);
  
  
}
