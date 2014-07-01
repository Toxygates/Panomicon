package t.admin.client;

import static t.admin.shared.MaintenanceConstants.*;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class BatchUploader extends UploadDialog {
	protected MaintenanceServiceAsync maintenanceService = (MaintenanceServiceAsync) GWT
			.create(MaintenanceService.class);
	
	private UploadWrapper metadata, normalized, fold, calls, foldCalls, foldP; 
	
	private Button proceed, cancel;	
	private TextArea comments;
	
	protected void makeGUI(VerticalPanel vp) {
	
		Label l = new Label("ID (no spaces, must be unique)");
		vp.add(l);
		final TextBox nameText = new TextBox();
		vp.add(nameText);
		
		metadata = new UploadWrapper(this, "Metadata file (TSV)", 
				metaPrefix, "tsv");	
		normalized = new UploadWrapper(this, "Normalized intensity data file (CSV)", 
				niPrefix, "csv");
		calls = new UploadWrapper(this, "Calls file (CSV) (optional)", 
				callPrefix, "csv");
		fold = new UploadWrapper(this, "Fold change data file (CSV)", 
				foldPrefix, "csv");
		foldCalls = new UploadWrapper(this, "Fold change calls file (CSV) (optional)",
				foldCallPrefix, "csv");
		foldP = new UploadWrapper(this, "Fold change p-values (CSV) (optional)",
				foldPPrefix, "csv");


		vp.add(metadata);
		vp.add(normalized);
		vp.add(calls);
		vp.add(fold);
		vp.add(foldCalls);
		vp.add(foldP);
		
		vp.add(new Label("Comments"));
		comments = new TextArea();
		comments.setSize("400px", "100px");
		vp.add(comments);
		
		Command c = new Command("Proceed") {
			@Override 
			void run() { 
				if ((!calls.hasFile() || !foldCalls.hasFile()) && 
						!Window.confirm("One or both of the call data files are missing. " +
								"Upload batch without calls data (all values present)?")) {
					return;
				}
				
				if (!foldP.hasFile() && !Window.confirm("Upload batch without fold p-values?")) {
					return;
				}
				
				maintenanceService.addBatchAsync(nameText.getText(),
						comments.getText(),
						new TaskCallback("Upload batch") {

					@Override
					void onCompletion() {
						completed = true;
						cancel.setText("OK");
					}
				});
			}
		};
		
		proceed = Utils.makeButton(c);
		HorizontalPanel hp = new HorizontalPanel();
		hp.setSpacing(4);
		hp.add(proceed);
		
		c = new Command("Cancel") {
			@Override 
			void run() {
				onFinish();
			}
		};
		cancel = Utils.makeButton(c);
		hp.add(cancel);		
		vp.add(hp);	
		updateStatus();
	}
	
	public void updateStatus() {
		if (metadata.hasFile() && normalized.hasFile() && fold.hasFile()) {
			proceed.setEnabled(true);
		} else {
			proceed.setEnabled(false);
		}
	}
}
