package t.admin.client;

import t.admin.shared.Batch;
import t.admin.shared.Instance;
import t.admin.shared.OperationResults;
import t.admin.shared.Platform;
import t.admin.shared.Progress;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface MaintenanceServiceAsync {

	void getBatches(AsyncCallback<Batch[]> callback);
	
	void getInstances(AsyncCallback<Instance[]> callback);
	
	void getPlatforms(AsyncCallback<Platform[]> callback);
	
	void tryAddBatch(String id, AsyncCallback<Void> callback);
	
	void tryAddPlatform(AsyncCallback<Void> callback);
	
	void tryDeleteBatch(String id, AsyncCallback<Boolean> callback);
	
	void tryDeletePlatform(String id, AsyncCallback<Boolean> calback);
	
	void updateBatch(Batch b, AsyncCallback<Void> callback);
	
	void getOperationResults(AsyncCallback<OperationResults> callback);
	
	void cancelTask(AsyncCallback<Void> callback);
	
	void getProgress(AsyncCallback<Progress> callback);
}
