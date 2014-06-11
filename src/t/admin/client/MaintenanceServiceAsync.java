package t.admin.client;

import t.admin.shared.AddBatchResult;
import t.admin.shared.AddPlatformResult;
import t.admin.shared.Progress;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface MaintenanceServiceAsync {

	void tryAddBatch(AsyncCallback<AddBatchResult> callback);
	
	void tryAddPlatform(AsyncCallback<AddPlatformResult> callback);
	
	void tryDeleteBatch(String id, AsyncCallback<Boolean> callback);
	
	void tryDeletePlatform(String id, AsyncCallback<Boolean> calback);
	
	void cancelTask(AsyncCallback<Void> callback);
	
	void getProgress(AsyncCallback<Progress> callback);
}
