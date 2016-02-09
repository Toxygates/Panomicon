package t.common.client.rpc;

import t.common.shared.maintenance.Batch;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface BatchOperationsAsync extends MaintenanceOperationsAsync {
  
  void getBatches(AsyncCallback<Batch[]> callback);
  
  void addBatchAsync(Batch b, AsyncCallback<Void> callback);

  void batchParameterSummary(Batch b, AsyncCallback<String[][]> callback);

  void deleteBatchAsync(String id, AsyncCallback<Void> callback);

}
