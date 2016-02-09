package t.viewer.client.rpc;

import t.common.shared.ManagedItem;
import t.common.shared.maintenance.MaintenanceException;
import t.common.shared.maintenance.OperationResults;
import t.common.shared.maintenance.Progress;

/**
 * Common maintenance operations for a RPC service.
 */
public interface MaintenanceOperations {

  /**
   * Cancel the current task, if any.
   */
  void cancelTask();


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
   * Get the status of the current task.
   * 
   * @return
   */
  Progress getProgress();
}
