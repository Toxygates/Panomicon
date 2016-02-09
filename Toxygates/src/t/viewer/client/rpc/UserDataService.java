package t.viewer.client.rpc;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * A service for managing user-editable data.
 * This is distinct from the admin interface, where all data can be managed.
 * Security is a concern.
 */
@RemoteServiceRelativePath("userData")
public interface UserDataService extends RemoteService, BatchOperations {


}
