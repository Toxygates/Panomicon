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

import static t.admin.shared.MaintenanceConstants.platformPrefix;
import t.common.client.Command;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

import static t.common.client.Utils.makeButton;

public class PlatformUploader extends UploadDialog {
	protected MaintenanceServiceAsync maintenanceService = (MaintenanceServiceAsync) GWT
			.create(MaintenanceService.class);
	
	private UploadWrapper platform;		
	private Button proceed, cancel;
	private RadioButton affyRadio, tRadio;
	private TextArea commentText;
	
	protected void makeGUI(VerticalPanel vp) {		
		Label l = new Label("ID (no spaces, must be unique)");
		vp.add(l);
		final TextBox nameText = new TextBox();
		vp.add(nameText);
		
		platform = new UploadWrapper(this, "Platform definition (CSV/TSV)", 
				platformPrefix, "tsv", "csv");
		
		vp.add(platform);
		
		l = new Label("File format");
		vp.add(l);
		affyRadio = makeRadio("type", "Affymetrix CSV");
		vp.add(affyRadio);
		tRadio = makeRadio("type", "T platform TSV");
		vp.add(tRadio);
		
		Command c = new Command("Proceed") {
			@Override 
			public void run() { 				
				boolean affyFormat = affyRadio.getValue();
				maintenanceService.addPlatformAsync(nameText.getText(),
						commentText.getText(),
						affyFormat, new TaskCallback("Add platform") {
					@Override
					void onCompletion() {
						completed = true;
						cancel.setText("OK");						
					}
				});
			}
		};
		
		vp.add(new Label("Comment"));
		
		commentText = new TextArea();
		commentText.setSize("400px", "100px");
		vp.add(commentText);
		
		proceed = makeButton(c);
		HorizontalPanel hp = new HorizontalPanel();
		hp.setSpacing(4);
		hp.add(proceed);
		cancel = new Button("Cancel");
		hp.add(cancel);
		cancel.addClickHandler(new ClickHandler() {			
			@Override
			public void onClick(ClickEvent event) {
				onFinish();
			}
		});
		
		vp.add(hp);
		updateStatus();
	}
	
	private RadioButton makeRadio(String group, String label) {
		RadioButton r = new RadioButton(group, label);
		r.addValueChangeHandler(new ValueChangeHandler<Boolean>() {			
			@Override
			public void onValueChange(ValueChangeEvent<Boolean> event) {
				updateStatus();
				
			}
		});
		return r;
	}
	 
	public void updateStatus() {
		if (platform.hasFile() && (affyRadio.getValue() || tRadio.getValue())) {
			proceed.setEnabled(true);
		} else {
			proceed.setEnabled(false);
		}
	}
	
}
