package otgviewer.client.dialog;

import otgviewer.client.Utils;
import otgviewer.client.components.DataListenerWidget;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.Widget;

abstract public class InteractionDialog {

	protected DialogBox db;
	protected final DataListenerWidget parent;
	
	public InteractionDialog(DataListenerWidget parent) {
		this.parent = parent;
	}
	
	abstract protected Widget content();
	 
	protected void userProceed() { 
		db.setVisible(false);
	}
	
	protected void userCancel() {
		db.setVisible(false);
	}
	
	public void display(String caption, DialogPosition pos) {
		db = Utils.displayInPopup(caption, content(), pos);
	}
	
	protected ClickHandler cancelHandler() {
		return new ClickHandler() {			
			@Override
			public void onClick(ClickEvent event) {
				userCancel();				
			}
		};
	}
}
