package t.admin.client;

import t.admin.shared.MaintenanceException;
import t.admin.shared.OperationResults;
import t.admin.shared.Progress;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("maintenance")
public interface MaintenanceService extends RemoteService {

	/**
	 * Try to add a batch, based on files that were previously uploaded.
	 * The results can be obtained after completion by using getOperationResults.
	 */
	void tryAddBatch(String id) throws MaintenanceException;
	
	/**
	 * Try to add a platform, based on files that were previously uploaded.
	 * The results can be obtained after completion by using getOperationResults.
	 */ 
	void tryAddPlatform();
	
	boolean tryDeleteBatch(String id);
	
	boolean tryDeletePlatform(String id);

	/**
	 * The results of the last completed long-running operation.
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
