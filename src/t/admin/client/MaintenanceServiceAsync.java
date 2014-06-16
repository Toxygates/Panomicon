package t.admin.client;

import t.admin.shared.OperationResults;
import t.admin.shared.Progress;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface MaintenanceServiceAsync {

	void tryAddBatch(String id, AsyncCallback<Void> callback);
	
	void tryAddPlatform(AsyncCallback<Void> callback);
	
	void tryDeleteBatch(String id, AsyncCallback<Boolean> callback);
	
	void tryDeletePlatform(String id, AsyncCallback<Boolean> calback);
	
	void getOperationResults(AsyncCallback<OperationResults> callback);
	
	void cancelTask(AsyncCallback<Void> callback);
	
	void getProgress(AsyncCallback<Progress> callback);
}
