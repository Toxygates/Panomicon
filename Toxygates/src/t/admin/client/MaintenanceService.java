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

package t.admin.client;

import t.admin.shared.Batch;
import t.admin.shared.Instance;
import t.admin.shared.MaintenanceException;
import t.admin.shared.OperationResults;
import t.admin.shared.Platform;
import t.admin.shared.Progress;
import t.common.shared.Dataset;
import t.common.shared.ManagedItem;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("maintenance")
public interface MaintenanceService extends RemoteService {

  Batch[] getBatches();

  Instance[] getInstances();

  Platform[] getPlatforms();

  Dataset[] getDatasets();

  /**
   * Try to add a batch, based on files that were previously uploaded. The results can be obtained
   * after completion by using getOperationResults. Only title and comment are read from b.
   */
  void addBatchAsync(Batch b) throws MaintenanceException;

  /**
   * Try to add a platform, based on files that were previously uploaded. The results can be
   * obtained after completion by using getOperationResults. Only title and comment are read from p.
   */
  void addPlatformAsync(Platform p, boolean affymetrixFormat) throws MaintenanceException;

  // Add other ManagedItems
  void add(ManagedItem i) throws MaintenanceException;


  /**
   * Delete a batch.
   * 
   * @param id
   */
  void deleteBatchAsync(String id) throws MaintenanceException;

  void deletePlatformAsync(String id) throws MaintenanceException;

  // TODO use a simple delete-method for synchronous deletion of ManagedItem
  void deleteInstance(String id) throws MaintenanceException;

  void deleteDataset(String id) throws MaintenanceException;

  /**
   * Modify the item, altering fields such as visibility and comment.
   * 
   * @param b
   */
  void update(ManagedItem i) throws MaintenanceException;


  /**
   * The results of the last completed asynchronous operation.
   * 
   * @return
   */
  OperationResults getOperationResults() throws MaintenanceException;

  /**
   * Cancel the current task, if any.
   */
  void cancelTask();

  /**
   * Get the status of the current task.
   * 
   * @return
   */
  Progress getProgress();
  // etc. TODO
}
