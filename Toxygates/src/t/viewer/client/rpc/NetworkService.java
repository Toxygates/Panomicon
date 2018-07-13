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

import t.viewer.shared.TimeoutException;
import t.viewer.shared.mirna.MirnaSource;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("network")
public interface NetworkService extends RemoteService {
  
  /**
   * Set the desired miRNA association sources. In the objects passed in,
   * only the ID string and the score threshold (lower bound) are used.
   * The choices are persisted in the user's server side session.
   * @param sources
   * @throws TimeoutException
   */
  void setMirnaSources(MirnaSource[] sources) throws TimeoutException;
  
  
}
