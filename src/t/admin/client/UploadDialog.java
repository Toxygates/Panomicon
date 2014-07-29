package t.admin.client;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.VerticalPanel;

abstract public class UploadDialog extends Composite {
	protected MaintenanceServiceAsync maintenanceService = (MaintenanceServiceAsync) GWT
			.create(MaintenanceService.class);
	
	protected boolean completed;
	protected List<UploadWrapper> uploaders = new ArrayList<UploadWrapper>();
	
	public UploadDialog() {
		VerticalPanel vp = new VerticalPanel();
		initWidget(vp);
		makeGUI(vp);
	}
	
	protected void resetAll() {
		for (UploadWrapper u: uploaders) {
			u.reset();
		}
	}
	
	abstract protected void makeGUI(VerticalPanel vp);
	
	public void updateStatus() { }
	
	public void onOK() { }
	
	public void onCancel() { }

	void onFinish() {
		if (completed) {
			onOK();
		} else {
			onCancel();
		}
	}

}
