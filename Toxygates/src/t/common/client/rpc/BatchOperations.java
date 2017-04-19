/*
 * Copyright (c) 2012-2017 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
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

package t.common.client.rpc;

import javax.annotation.Nullable;

import t.common.shared.maintenance.Batch;
import t.common.shared.maintenance.MaintenanceException;

/**
 * Management operations for batches.
 */
public interface BatchOperations extends MaintenanceOperations {
  /**
   * Get the batches that the user is allowed to manage.
   * @param datasets The datasets to request batches from, or null/empty to get all batches 
   * (if this is allowed)
   * @return Batches in the datasets
   * @throws MaintenanceException
   */
  Batch[] getBatches(@Nullable String[] datasets) throws MaintenanceException;
  
  void addBatchAsync(Batch b) throws MaintenanceException;

  /**
   * Get parameter summaries for samples in a batch.
   * The result is a row-major table. The first row will be column headers.
   * @param b
   * @return
   */
  String[][] batchParameterSummary(Batch b) throws MaintenanceException;

  /**
   * Delete a batch.
   * @param b
   * @throws MaintenanceException
   */
  void deleteBatchAsync(Batch b) throws MaintenanceException;
}
