package t.admin.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

import t.admin.shared.AddBatchResult;
import t.admin.shared.AddPlatformResult;

public interface MaintenanceServiceAsync {

	void tryAddBatch(AsyncCallback<AddBatchResult> callback);
	
	void tryAddPlatform(AsyncCallback<AddPlatformResult> callback);
	
	void tryDeleteBatch(String id, AsyncCallback<Boolean> callback);
	
	void tryDeletePlatform(String id, AsyncCallback<Boolean> calback);
}
