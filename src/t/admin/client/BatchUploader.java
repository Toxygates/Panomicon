package t.admin.client;

import static t.admin.shared.MaintenanceConstants.callPrefix;
import static t.admin.shared.MaintenanceConstants.niPrefix;
import static t.admin.shared.MaintenanceConstants.mas5Prefix;
import static t.admin.shared.MaintenanceConstants.metaPrefix;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class BatchUploader extends UploadDialog {
	protected MaintenanceServiceAsync maintenanceService = (MaintenanceServiceAsync) GWT
			.create(MaintenanceService.class);
	
	private UploadWrapper metadata; 
	private UploadWrapper normalized; 
	private UploadWrapper mas5;
	private UploadWrapper calls;
	
	private Button proceed;
	
	protected void makeGUI(VerticalPanel vp) {
	
		Label l = new Label("ID (no spaces, must be unique)");
		vp.add(l);
		final TextBox nameText = new TextBox();
		vp.add(nameText);
		
		metadata = new UploadWrapper(this, "Metadata file (TSV)", 
				metaPrefix, "tsv");	
		normalized = new UploadWrapper(this, "Normalized intensity data file (CSV)", 
				niPrefix, "csv");
		mas5 = new UploadWrapper(this, "MAS5 normalized data file (for fold change) (CSV)", 
				mas5Prefix, "csv");
		calls = new UploadWrapper(this, "Calls file (CSV)", 
				callPrefix, "csv");

		vp.add(metadata);
		vp.add(normalized);
		vp.add(mas5);
		vp.add(calls);
		
		Command c = new Command("Proceed") {
			@Override 
			void run() { 
				maintenanceService.addBatchAsync(nameText.getText(),
						"",
						new TaskCallback("Upload batch"));						
			}
		};
		
		proceed = Utils.makeButton(c);
		HorizontalPanel hp = new HorizontalPanel();
		hp.setSpacing(4);
		hp.add(proceed);
		
		c = new Command("Cancel") {
			@Override 
			void run() {
				onCancel();
			}
		};
		hp.add(Utils.makeButton(c));		
		vp.add(hp);	
		updateStatus();
	}
	
	public void updateStatus() {
		if (metadata.hasFile() && normalized.hasFile() && mas5.hasFile() &&
				calls.hasFile()) {
			proceed.setEnabled(true);
		} else {
			proceed.setEnabled(false);
		}
	}
	
	public void onOK() { }
	
	public void onCancel() { }
}
