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
	
	void addBatchAsync(String id, String comment, AsyncCallback<Void> callback);
	
	void addPlatformAsync(String id, String comment, boolean affymetrixFormat, 
			AsyncCallback<Void> callback);
	
	void addInstance(Instance i, AsyncCallback<Void> callback);

	void deleteBatchAsync(String id, AsyncCallback<Void> callback);
	
	void deletePlatformAsync(String id, AsyncCallback<Void> calback);
	
	void deleteInstance(String id, AsyncCallback<Void> callback);
	
	void updateBatch(Batch b, AsyncCallback<Void> callback);
	
	void getOperationResults(AsyncCallback<OperationResults> callback);
	
	void cancelTask(AsyncCallback<Void> callback);
	
	void getProgress(AsyncCallback<Progress> callback);
}
