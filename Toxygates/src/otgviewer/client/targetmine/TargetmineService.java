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

package otgviewer.client.targetmine;

import t.common.shared.StringList;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("targetmine")
public interface TargetmineService extends RemoteService {

  /**
   * Import gene lists from a targetmine user account.
   * 
   * @param user
   * @param pass
   * @param asProbes if true, the items will be imported as affymetrix probes. If false, as genes.
   * @return
   */
  public StringList[] importTargetmineLists(String user, String pass, boolean asProbes);

  public void exportTargetmineLists(String user, String pass, StringList[] lists, boolean replace);

  public void enrichment(String user, String pass, StringList list);

}
