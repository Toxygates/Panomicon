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

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class BatchUploader extends Composite implements UploadManager {
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
		final TextBox nameText = new TextBox();
		vp.add(nameText);
		
		UploadWrapper uploader = new UploadWrapper(this, "Metadata file (TSV)", 
				metaPrefix, "tsv");				
		vp.add(uploader);
		
		uploader = new UploadWrapper(this, "Normalized intensity data file (CSV)", 
				niPrefix, "csv");
		vp.add(uploader);
		
		uploader = new UploadWrapper(this, "MAS5 normalized data file (for fold change) (CSV)", 
				mas5Prefix, "csv");
		vp.add(uploader);
		
		uploader = new UploadWrapper(this, "Calls file (CSV)", 
				callPrefix, "csv");
		vp.add(uploader);
		
		List<Command> commands = new ArrayList<Command>();
		commands.add(new Command("OK") {
			@Override 
			void run() { 
				maintenanceService.tryAddBatch(nameText.getText(),
						new AsyncCallback<Void>() {					
					@Override
					public void onSuccess(Void result) {
						final DialogBox db = new DialogBox();
						ProgressDisplay pd = new ProgressDisplay("Add batch") {
							@Override
							protected void onDone() {
								db.hide();
							}							
						};
						db.setWidget(pd);
						db.setText("Progress");
						db.show();
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
			void run() {
				onCancel();
			}
		});
		
		vp.add(makeButtons(commands));	
	}
	
	public void updateStatus() {
		// enable/disable buttons
	}
	
	public void onOK() {
		
	}
	
	public void onCancel() {
		
	}
}
