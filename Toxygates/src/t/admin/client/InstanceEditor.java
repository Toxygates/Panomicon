package t.admin.client;

import java.util.Date;

import t.admin.shared.AccessPolicy;
import t.admin.shared.Instance;
import t.common.client.components.EnumSelector;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;

public class InstanceEditor extends ManagedItemEditor {
	
	private final TextBox idText, commentText, roleText;
	private final EnumSelector<AccessPolicy> policySelector;
	
	public InstanceEditor() {
		super();
		
		idText = addLabelledTextBox("ID");
		commentText = addLabelledTextBox("Comment");
				
		Label l = new Label("Access policy");
		vp.add(l);
		
		policySelector = new EnumSelector<AccessPolicy>() {
			@Override
			protected AccessPolicy[] values() {
				return AccessPolicy.values();
			}
			
		};
		vp.add(policySelector);
		
		roleText = addLabelledTextBox("Tomcat role name (for password protection only)");		
		roleText.setText("toxygates-test");
		
		addCommands();				
	}	
	
	@Override
	protected void triggerEdit() {
		Instance i = new Instance(idText.getValue(), commentText.getValue(), new Date());
		AccessPolicy ap = policySelector.value();				
		i.setAccessPolicy(ap, roleText.getText());		
		maintenanceService.addInstance(i, editCallback());
	}
}
