package t.admin.client;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DialogBox;

public class TaskCallback implements AsyncCallback<Void> {	
	final String title;

	public TaskCallback(String title) {
		this.title = title;
	}

	@Override
	public void onSuccess(Void result) {
		final DialogBox db = new DialogBox(false, true);
		ProgressDisplay pd = new ProgressDisplay(title) {
			@Override
			protected void onDone() {
				db.hide();	
				onCompletion();
			}
			
			@Override
			protected void onCancelled() {
				onFailure();
			}
		};
		db.setWidget(pd);
		db.setText("Progress");
		db.show();				
	}

	@Override
	public void onFailure(Throwable caught) {
		Window.alert("Failure: " + caught.getMessage());
		onFailure();
	}
	
	void onCompletion() {
		
	}
	
	void onFailure() {
		
	}

}
