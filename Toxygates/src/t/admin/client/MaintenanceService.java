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

package t.admin.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import t.admin.shared.PlatformType;
import t.common.client.rpc.BatchOperations;
import t.shared.common.*;
import t.shared.common.maintenance.Instance;
import t.shared.common.maintenance.MaintenanceException;

@RemoteServiceRelativePath("maintenance")
public interface MaintenanceService extends BatchOperations, RemoteService {

  Instance[] getInstances();

  Platform[] getPlatforms();

  Dataset[] getDatasets();

  /**
   * Try to add a platform, based on files that were previously uploaded. The results can be
   * obtained after completion by using getOperationResults. Only title and comment are read from p.
   */
  void addPlatformAsync(Platform p, PlatformType pt) throws MaintenanceException;

  // Add other ManagedItems
  void add(ManagedItem i) throws MaintenanceException;


  void deletePlatformAsync(String id) throws MaintenanceException;

  /**
   * Synchronously delete an item (except for platform and batch)
   * @throws MaintenanceException
   */
  void delete(ManagedItem i) throws MaintenanceException;

}
