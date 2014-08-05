package otgviewer.client.targetmine;

import t.viewer.shared.StringList;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface TargetmineServiceAsync {
	
	public void importTargetmineLists(String user, String pass, 
			boolean asProbes, AsyncCallback<StringList[]> callback);
	
	public void exportTargetmineLists(String user, String pass, 
			StringList[] lists, boolean replace, AsyncCallback<Void> callback);
}
