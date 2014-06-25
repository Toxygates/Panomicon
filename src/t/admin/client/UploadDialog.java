package t.admin.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.VerticalPanel;

abstract public class UploadDialog extends Composite {
	protected MaintenanceServiceAsync maintenanceService = (MaintenanceServiceAsync) GWT
			.create(MaintenanceService.class);
	
	protected boolean completed;
	
	public UploadDialog() {
		VerticalPanel vp = new VerticalPanel();
		initWidget(vp);
		makeGUI(vp);
	}
	
	abstract protected void makeGUI(VerticalPanel vp);
	
	public void updateStatus() { }
	
	public void onOK() { }
	
	public void onCancel() { }
	
	void onCompletion() {}
	
	void onFailure() {}
	
	void onFinish() {
		if (completed) {
			onOK();
		} else {
			onCancel();
		}
	}

}
