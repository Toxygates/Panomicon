package t.admin.client;

import static t.admin.client.Utils.makeButtons;
import static t.admin.shared.MaintenanceConstants.callPrefix;
import static t.admin.shared.MaintenanceConstants.mas5Prefix;
import static t.admin.shared.MaintenanceConstants.metaPrefix;
import static t.admin.shared.MaintenanceConstants.niPrefix;
import gwtupload.client.IUploader;
import gwtupload.client.IUploader.OnCancelUploaderHandler;
import gwtupload.client.IUploader.OnFinishUploaderHandler;
import gwtupload.client.IUploader.OnStartUploaderHandler;
import gwtupload.client.SingleUploader;
import gwtupload.client.Uploader;

import java.util.ArrayList;
import java.util.List;

import t.admin.shared.AddBatchResult;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class BatchUploader extends Composite {
	protected MaintenanceServiceAsync maintenanceService = (MaintenanceServiceAsync) GWT
			.create(MaintenanceService.class);
	
	boolean metadataIsOK;
	boolean IDIsOK;
	boolean normalizedIsOK;
	boolean mas5IsOK;
	boolean callsIsOK;
	
	List<UploadWrapper> uploaders = new ArrayList<UploadWrapper>();
	
	public BatchUploader() {
		VerticalPanel vp = new VerticalPanel();
		initWidget(vp);
		
		Label l = new Label("ID (no spaces, must be unique)");
		vp.add(l);
		TextBox nameText = new TextBox();
		vp.add(nameText);
		
		UploadWrapper uploader = new UploadWrapper("Metadata file (TSV)", 
				metaPrefix, "tsv");				
		vp.add(uploader);
		
		uploader = new UploadWrapper("Normalized intensity data file (TSV)", 
				niPrefix, "tsv");
		vp.add(uploader);
		
		uploader = new UploadWrapper("MAS5 normalized data file (for fold change) (TSV)", 
				mas5Prefix, "tsv");
		vp.add(uploader);
		
		uploader = new UploadWrapper("Calls file (TSV)", 
				callPrefix, "tsv");
		vp.add(uploader);
		
		List<Command> commands = new ArrayList<Command>();
		commands.add(new Command("OK") {
			@Override 
			void run() { 
				maintenanceService.tryAddBatch(new AsyncCallback<AddBatchResult>() {					
					@Override
					public void onSuccess(AddBatchResult result) {
						Window.alert("Success");
					}
					
					@Override
					public void onFailure(Throwable caught) {
						Window.alert("Failed to add batch: " + caught.getMessage());
					}
				});
			}
		});
		commands.add(new Command("Cancel") {
			@Override 
			void run() { onCancel(); }
		});
		
		vp.add(makeButtons(commands));	
	}
	
	class UploadWrapper extends Composite {
		Uploader u;		
		boolean finished;		
		Label statusLabel = new Label();		
		VerticalPanel vp = new VerticalPanel();
		UploadWrapper(String description, String prefix, String ... extensions) {
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
					setFailure();					
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
					setFailure();										
				}
			});
			vp.add(u);
			vp.add(statusLabel);
			setFailure();
		}
		
		void setFinished() {
			finished = true;
			statusLabel.setText("OK");
			statusLabel.setStylePrimaryName("success");
			updateStatus();
		}
		
		void setFailure() {
			finished = false;
			statusLabel.setStylePrimaryName("failure");
			statusLabel.setText("Please upload a file");
			updateStatus();
		}
	}
	
	private void updateStatus() {
		
	}
	
	public void onOK() {
		
	}
	
	public void onCancel() {
		
	}
}
