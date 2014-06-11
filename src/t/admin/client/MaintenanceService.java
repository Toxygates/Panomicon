package t.admin.client;

import t.admin.shared.AddBatchResult;
import t.admin.shared.AddPlatformResult;
import t.admin.shared.Progress;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("maintenance")
public interface MaintenanceService extends RemoteService {

	/**
	 * Try to add a batch, based on files that were previously uploaded.
	 * @return
	 */
	AddBatchResult tryAddBatch();
	
	/**
	 * Try to add a platform, based on files that were previously uploaded.
	 * @return
	 */
	AddPlatformResult tryAddPlatform();
	
	boolean tryDeleteBatch(String id);
	
	boolean tryDeletePlatform(String id);

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
