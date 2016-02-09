package t.common.client.rpc;

import t.common.shared.ManagedItem;
import t.common.shared.maintenance.OperationResults;
import t.common.shared.maintenance.Progress;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Async version of the common maintenance operations.
 */
public interface MaintenanceOperationsAsync {

  void update(ManagedItem i, AsyncCallback<Void> callback);

  void getOperationResults(AsyncCallback<OperationResults> callback);

  void cancelTask(AsyncCallback<Void> callback);

  void getProgress(AsyncCallback<Progress> callback);

}
