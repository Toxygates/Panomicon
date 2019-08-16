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

package t.viewer.shared.network;

import java.io.Serializable;

import t.viewer.shared.ManagedMatrixInfo;

/**
 * Information about a network loaded in a session on the server.
 * Corresponds loosely to ManagedMatrixInfo for single tables.
 *
 */
@SuppressWarnings("serial")
public class NetworkInfo implements Serializable {

  //GWT constructor
  NetworkInfo() {}
  
  public NetworkInfo(ManagedMatrixInfo mainInfo,
      ManagedMatrixInfo sideInfo,
      Network network) {
    this.mainInfo = mainInfo;
    this.sideInfo = sideInfo;
    this.network = network;
  }
  
  private ManagedMatrixInfo mainInfo, sideInfo;
  
  /**
   * Information about the loaded main matrix  
   * @return
   */
  public ManagedMatrixInfo mainInfo() { return mainInfo; }
  /**
   * Information about the loaded side matrix
   * @return
   */
  public ManagedMatrixInfo sideInfo() { return sideInfo; }
  
  private Network network;
  
  /**
   * The interaction network. The initial view is provided for convenience.
   * Currently, after sorting or filtering, this view will no longer be accurate.
   * @return
   */
  public Network network() { return network; }
}
