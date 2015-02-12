package t.admin.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import t.admin.shared.AccessPolicy;
import t.admin.shared.Instance;
import t.common.client.Command;
import static t.common.client.Utils.makeButtons;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;


abstract class ManagedItemEditor extends Composite {

	protected MaintenanceServiceAsync maintenanceService = (MaintenanceServiceAsync) GWT
			.create(MaintenanceService.class);
	
	
	protected VerticalPanel vp;
	
	public ManagedItemEditor() {		
		vp = new VerticalPanel();
		initWidget(vp);
	}
	
	protected TextBox addLabelledTextBox(String label) {
		Label l = new Label(label);
		vp.add(l);
		TextBox text = new TextBox();
		vp.add(text);
		return text;
	}

	protected void addCommands() {
		List<Command> cmds = new ArrayList<Command>();
		Command c = new Command("OK") {
			@Override
			public void run() { triggerEdit(); }			
		};
		cmds.add(c);
		
		c = new Command("Cancel") {
			@Override
			public void run() { onAbort(); }
			
		};
		cmds.add(c);
		
		Widget btns = makeButtons(cmds);
		vp.add(btns);
	}
	
	protected abstract void triggerEdit();
	
	protected AsyncCallback<Void> editCallback() {
		return new AsyncCallback<Void>() {			
			@Override
			public void onSuccess(Void result) {
				Window.alert("Operation successful");						
				onFinish();						
			}
			
			@Override
			public void onFailure(Throwable caught) {
				Window.alert("Operation failed: " + caught.getMessage());	
				onAbort();						
			}
		};				
	}
	
	/**
	 * Called when the edit operation has successfully finished.
	 */
	protected void onFinish() {}
	
	/**
	 * Called when the edit operation was cancelled (by the user
	 * or due to an error)
	 */
	protected void onAbort() {}
}
