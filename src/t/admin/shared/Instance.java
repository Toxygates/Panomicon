package t.admin.shared;

import java.util.Date;

public class Instance extends ManagedItem {
	
	private AccessPolicy policy;
	
	public Instance() { }

	public Instance(String title, String comment, Date date) {
		super(title, comment, date);
	}
	
	public void setAccessPolicy(AccessPolicy p) { this.policy = p; }
	public AccessPolicy getAccessPolicy() { return policy; }
}
