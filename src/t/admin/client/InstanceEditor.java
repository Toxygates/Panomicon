package t.admin.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import t.admin.shared.AccessPolicy;
import t.admin.shared.Instance;
import t.common.client.components.EnumSelector;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class InstanceEditor extends Composite {
	protected MaintenanceServiceAsync maintenanceService = (MaintenanceServiceAsync) GWT
			.create(MaintenanceService.class);
	
	public InstanceEditor() {
		VerticalPanel vp = new VerticalPanel();
		initWidget(vp);
		
		Label l = new Label("ID");
		vp.add(l);
		
		final TextBox idText = new TextBox();
		vp.add(idText);
		
		l = new Label("Comment");
		vp.add(l);
		
		final TextBox commentText = new TextBox();
		vp.add(commentText);
		
		l = new Label("Access policy");
		vp.add(l);
		
		final EnumSelector<AccessPolicy> policySelector = new EnumSelector<AccessPolicy>() {
			@Override
			protected AccessPolicy[] values() {
				return AccessPolicy.values();
			}
			
		};
		vp.add(policySelector);
		
		l = new Label("Tomcat role name (for password protection only)");
		vp.add(l);
		
		final TextBox roleText = new TextBox();
		roleText.setText("toxygates-test");
		vp.add(roleText);
		
		List<Command> cmds = new ArrayList<Command>();
		Command c = new Command("OK") {
			@Override
			void run() {
				Instance i = new Instance(idText.getValue(), commentText.getValue(), new Date());
				AccessPolicy ap = policySelector.value();				
				i.setAccessPolicy(ap, roleText.getText());
				
				maintenanceService.addInstance(i, new AsyncCallback<Void>() {
					
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
				});				
			}			
		};
		cmds.add(c);
		
		c = new Command("Cancel") {
			@Override
			void run() {
				onAbort();				
			}
			
		};
		cmds.add(c);
		
		Widget btns = Utils.makeButtons(cmds);
		vp.add(btns);
	}	

	protected void onFinish() {}
	protected void onAbort() {}
}
