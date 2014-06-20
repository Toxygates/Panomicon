package t.admin.client;

import gwtupload.client.IUploader;
import gwtupload.client.SingleUploader;
import gwtupload.client.Uploader;
import gwtupload.client.IUploader.OnCancelUploaderHandler;
import gwtupload.client.IUploader.OnFinishUploaderHandler;
import gwtupload.client.IUploader.OnStartUploaderHandler;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

class UploadWrapper extends Composite {
	Uploader u;		
	boolean finished;		
	Label statusLabel = new Label();		
	VerticalPanel vp = new VerticalPanel();
	UploadDialog manager;
	
	UploadWrapper(UploadDialog manager, String description, 
			String prefix, String ... extensions) {
		this.manager = manager;
		initWidget(vp);		
		Label l = new Label(description);
		vp.add(l);
		vp.setStylePrimaryName("uploader");
		
		u = new SingleUploader();
		u.setFileInputPrefix(prefix);
		u.setValidExtensions(extensions);
		u.setAutoSubmit(true);
		
		u.addOnStartUploadHandler(new OnStartUploaderHandler() {				
			@Override
			public void onStart(IUploader uploader) {
				setFailure(true);					
				statusLabel.setText("In progress");									
			}
		});
		u.addOnFinishUploadHandler(new OnFinishUploaderHandler() {				
			@Override
			public void onFinish(IUploader uploader) {
				setFinished();					
			}
		});	
		u.addOnCancelUploadHandler(new OnCancelUploaderHandler() {				
			@Override
			public void onCancel(IUploader uploader) {
				setFailure(true);										
			}
		});
		vp.add(u);
		vp.add(statusLabel);
		setFailure(false);
	}
	
	void setFinished() {
		finished = true;
		statusLabel.setText("OK");
		statusLabel.setStylePrimaryName("success");
		manager.updateStatus();
	}
	
	void setFailure(boolean signal) {
		finished = false;
		statusLabel.setStylePrimaryName("failure");
		statusLabel.setText("Please upload a file");
		if (signal) {
			manager.updateStatus();
		}
	}
	
	boolean hasFile() { return finished; }
}