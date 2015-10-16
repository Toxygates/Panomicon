/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
 * (NIBIOHN), Japan.
 *
 * This file is part of Toxygates.
 *
 * Toxygates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Toxygates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Toxygates. If not, see <http://www.gnu.org/licenses/>.
 */

package t.admin.client;

import static t.admin.shared.MaintenanceConstants.callPrefix;
import static t.admin.shared.MaintenanceConstants.metaPrefix;
import static t.admin.shared.MaintenanceConstants.dataPrefix;
import t.common.client.Command;
import static t.common.client.Utils.makeButton;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class BatchUploader extends UploadDialog {
	protected MaintenanceServiceAsync maintenanceService = (MaintenanceServiceAsync) GWT
			.create(MaintenanceService.class);
	
	private UploadWrapper metadata, data, calls; 
	
	private Button proceed, cancel;	
	private TextArea comments;
	
	protected void makeGUI(VerticalPanel vp) {
	
		Label l = new Label("ID (no spaces, must be unique)");
		vp.add(l);
		final TextBox nameText = new TextBox();
		vp.add(nameText);
		
		metadata = new UploadWrapper(this, "Metadata file (TSV)", 
				metaPrefix, "tsv");	
		uploaders.add(metadata);
		data = new UploadWrapper(this, "Normalized data file (CSV)", 
				dataPrefix, "csv");
		uploaders.add(data);
		calls = new UploadWrapper(this, "Affymetrix calls file (CSV) (optional)", 
				callPrefix, "csv");
		uploaders.add(calls);
		
		HorizontalPanel hp = new HorizontalPanel();
		hp.add(metadata);
		hp.add(data);
		vp.add(hp);
		
		hp = new HorizontalPanel();
		hp.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
		hp.setWidth("100%");
		hp.add(calls);		
		vp.add(hp);
		
		vp.add(new Label("Comments"));
		comments = new TextArea();
		comments.setSize("400px", "100px");
		vp.add(comments);
		
		Command c = new Command("Proceed") {
			@Override 
			public void run() { 
				if (!calls.hasFile() && 
						!Window.confirm("The Affymetrix calls file is missing. " +
								"Upload batch without calls data (all values present)?")) {
					return;
				}
				
				maintenanceService.addBatchAsync(nameText.getText(),
						comments.getText(),
						new TaskCallback("Upload batch") {

					@Override
					void onCompletion() {
						completed = true;
						cancel.setText("Close");
						resetAll();
					}
					
					@Override
					void onFailure() {
						resetAll();
					}
				});
			}
		};
		
		proceed = makeButton(c);
		hp = new HorizontalPanel();
		hp.setSpacing(4);
		hp.add(proceed);
		
		c = new Command("Cancel") {
			@Override 
			public void run() {
				onFinish();
			}
		};
		cancel = makeButton(c);
		hp.add(cancel);		
		vp.add(hp);	
		updateStatus();
	}
	
	public void updateStatus() {
		if (metadata.hasFile() && data.hasFile()) {
			proceed.setEnabled(true);
		} else {
			proceed.setEnabled(false);
		}
	}
}
