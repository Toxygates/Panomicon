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
import t.viewer.shared.TimeoutException;
import t.viewer.shared.mirna.MirnaSource;
import t.viewer.shared.network.*;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("network")
public interface NetworkService extends RemoteService {

  /**
   * Prefix for matrix IDs in network state. To be prepended by client.
   */
  final static String tablePrefix = "network.";
  
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
   * @param Number of rows on the first page of the main table.
   * @return Information about the loaded network.
   */
  NetworkInfo loadNetwork(String mainId, List<Group> mainColumns, 
                          String[] mainProbes,
                          String sideId, List<Group> sideColumns, 
                          ValueType typ, int mainPageSize);
  

  /**
   * Serialize an interaction network to a downloadable file. 
   * @param network the network to serialize.
   * @param format the format to use for serialization.
   * @return a downloadable URL.
   */
  String prepareNetworkDownload(Network network, Format format, 
      String messengerWeightColumn, String microWeightColumn);
  
  
}
