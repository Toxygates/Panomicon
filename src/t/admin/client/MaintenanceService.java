package t.admin.client;

import t.admin.shared.Batch;
import t.admin.shared.Instance;
import t.admin.shared.MaintenanceException;
import t.admin.shared.OperationResults;
import t.admin.shared.Platform;
import t.admin.shared.Progress;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("maintenance")
public interface MaintenanceService extends RemoteService {

	Batch[] getBatches();
	
	Instance[] getInstances();
	
	Platform[] getPlatforms();
	
	/**
	 * Try to add a batch, based on files that were previously uploaded.
	 * The results can be obtained after completion by using getOperationResults.
	 */
	void addBatchAsync(String id, String comment) throws MaintenanceException;
	
	/**
	 * Try to add a platform, based on files that were previously uploaded.
	 * The results can be obtained after completion by using getOperationResults.
	 */ 
	void addPlatformAsync(String id, String comment, boolean affymetrixFormat);
	
	
	/**
	 * Delete a batch. 
	 * @param id
	 */
	void deleteBatchAsync(String id);
	
	void deletePlatformAsync(String id);

	/**
	 * Modify the batch, altering fields such as visibility and comment.
	 * @param b
	 */
	void updateBatch(Batch b);

	/**
	 * The results of the last completed asynchronous operation.
	 * @return
	 */
	OperationResults getOperationResults();
	
	/**
	 * Cancel the current task, if any.
	 */
	void cancelTask();

	/**
	 * Get the status of the current task.
	 * @return
	 */
	Progress getProgress();
	// etc. TODO
}
