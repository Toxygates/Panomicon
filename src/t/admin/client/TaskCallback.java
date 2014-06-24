package t.admin.client;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class TaskCallback implements AsyncCallback<Void> {	
	final String title;

	public TaskCallback(String title) {
		this.title = title;
	}

	@Override
	public void onSuccess(Void result) {
		Utils.showProgress(title);
	}

	@Override
	public void onFailure(Throwable caught) {
		Window.alert("Failure: " + caught.getMessage());
	}
	
	//TODO
	public void onTaskCompletion() {
		
	}
	
	//TODO
	public void onTaskFailure() {
		
	}

}
