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

package otgviewer.client.rpc;

import t.model.SampleClass;
import t.viewer.shared.TimeoutException;

import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("probe")
public interface ProbeService extends t.viewer.client.rpc.ProbeService {

  /**
   * Obtain probes that correspond to proteins targeted by the named compound.
   * 
   * @param compound
   * @param service Service to use for lookup (currently DrugBank or CHEMBL) 
   * (TODO it might be better to use an enum)
   * @param homologous Whether to use homologous genes (if not, only direct targets are returned)
   * @return
   */
  String[] probesTargetedByCompound(SampleClass sc, String compound, String service,
      boolean homologous) throws TimeoutException;

}
