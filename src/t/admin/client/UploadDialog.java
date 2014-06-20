package t.admin.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.VerticalPanel;

abstract public class UploadDialog extends Composite {
	protected MaintenanceServiceAsync maintenanceService = (MaintenanceServiceAsync) GWT
			.create(MaintenanceService.class);
	
	public UploadDialog() {
		VerticalPanel vp = new VerticalPanel();
		initWidget(vp);
		makeGUI(vp);
	}
	
	protected void showProgress(String title) {
		final DialogBox db = new DialogBox();
		ProgressDisplay pd = new ProgressDisplay(title) {
			@Override
			protected void onDone() {
				db.hide();
			}
		};
		db.setWidget(pd);
		db.setText("Progress");
		db.show();
	}
	
	abstract protected void makeGUI(VerticalPanel vp);
	
	public void updateStatus() { }
	
	public void onOK() { }
	
	public void onCancel() { }

}
