package otgviewer.client.targetmine;

import t.viewer.shared.StringList;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("targetmine")
public interface TargetmineService extends RemoteService {

	/**
	 * Import gene lists from a targetmine user account.
	 * This should not necessarily be in MatrixService...
	 * @param user
	 * @param pass
	 * @param asProbes if true, the items will be imported as affymetrix probes. If false, as genes.
	 * @return
	 */
	public StringList[] importTargetmineLists(String user, String pass, boolean asProbes);

	public void exportTargetmineLists(String user, String pass, 
			StringList[] lists, boolean replace);
	
}
