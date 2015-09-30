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

package otgviewer.client.dialog;

import otgviewer.client.components.DataListenerWidget;
import otgviewer.client.components.InputGrid;
import t.viewer.client.Utils;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

abstract public class TargetMineSyncDialog extends InteractionDialog {
	
	CheckBox replaceCheck = new CheckBox("Replace lists with identical names");
	InputGrid ig;
	String action;
	String url;
	
	public TargetMineSyncDialog(DataListenerWidget parent, String url, String action) {
		super(parent);
		this.action = action;
		this.url = url;
	}
	
	protected Widget content() {
		VerticalPanel vp = new VerticalPanel();
		vp.setWidth("400px");

		Label l = new Label("You must have a TargetMine account in order to use " +
				"this function. If you do not have one, you may create one " +
				"at " + url + ".");
		l.setWordWrap(true);
		vp.add(l);
		
		ig = new InputGrid("Account name (e-mail address)", "Password") {
			@Override
			protected TextBox initTextBox(int i) {
				if (i == 1) {
					return new PasswordTextBox();
				} else {
					return super.initTextBox(i);
				}
			}
		};		
		vp.add(ig);
		vp.add(replaceCheck);
		
		Button b = new Button(action);
		b.addClickHandler(new ClickHandler() {			
			@Override
			public void onClick(ClickEvent event) {
				if (ig.getValue(0).trim().equals("") || ig.getValue(1).trim().equals("")) {
					Window.alert("Please enter both your account name and password.");
				} else {
					userProceed(ig.getValue(0), ig.getValue(1), replaceCheck.getValue());
				}
			}
		});

		Button b2 = new Button("Cancel", cancelHandler());
		
		HorizontalPanel hp = Utils.mkHorizontalPanel(true, b, b2);
		vp.add(hp);
		return vp;
	}
	
	abstract protected void userProceed(String user, String pass, boolean replace);

}
