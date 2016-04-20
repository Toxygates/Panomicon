package t.common.client.rpc;

import javax.annotation.Nullable;

import t.common.shared.maintenance.Batch;
import t.common.shared.maintenance.MaintenanceException;

/**
 * Management operations for batches.
 */
public interface BatchOperations extends MaintenanceOperations {
  public Batch[] getBatches(@Nullable String dataset) throws MaintenanceException;
  
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
   * @param id
   * @throws MaintenanceException
   */
  void deleteBatchAsync(String id) throws MaintenanceException;
}
