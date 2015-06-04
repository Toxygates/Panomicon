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
import static t.admin.shared.MaintenanceConstants.foldCallPrefix;
import static t.admin.shared.MaintenanceConstants.foldPPrefix;
import static t.admin.shared.MaintenanceConstants.foldPrefix;
import static t.admin.shared.MaintenanceConstants.metaPrefix;
import static t.admin.shared.MaintenanceConstants.niPrefix;

import t.common.client.Command;
import static t.common.client.Utils.makeButton;

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
		uploaders.add(metadata);
		normalized = new UploadWrapper(this, "Normalized intensity data file (CSV)", 
				niPrefix, "csv");
		uploaders.add(normalized);
		calls = new UploadWrapper(this, "Calls file (CSV) (optional)", 
				callPrefix, "csv");
		uploaders.add(calls);
		fold = new UploadWrapper(this, "Fold change data file (CSV)", 
				foldPrefix, "csv");
		uploaders.add(fold);
		foldCalls = new UploadWrapper(this, "Fold change calls file (CSV) (optional)",
				foldCallPrefix, "csv");
		uploaders.add(foldCalls);
		foldP = new UploadWrapper(this, "Fold change p-values (CSV) (optional)",
				foldPPrefix, "csv");
		uploaders.add(foldP);

		HorizontalPanel hp = new HorizontalPanel();
		hp.add(metadata);
		hp.add(normalized);
		vp.add(hp);
		
		hp = new HorizontalPanel();
		hp.add(fold);
		hp.add(foldP);
		vp.add(hp);
		
		hp = new HorizontalPanel();
		hp.add(calls);
		hp.add(foldCalls);
		vp.add(hp);
		
		vp.add(new Label("Comments"));
		comments = new TextArea();
		comments.setSize("400px", "100px");
		vp.add(comments);
		
		Command c = new Command("Proceed") {
			@Override 
			public void run() { 
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
		if (metadata.hasFile() && normalized.hasFile() && fold.hasFile()) {
			proceed.setEnabled(true);
		} else {
			proceed.setEnabled(false);
		}
	}
}
