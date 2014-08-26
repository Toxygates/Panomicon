package t.admin.shared;

import java.util.Date;

public class Instance extends ManagedItem {
	
	private AccessPolicy policy;
	private String policyParameter;
	
	public Instance() { }

	public Instance(String title, String comment, Date date) {
		super(title, comment, date);
	}
	
	public void setAccessPolicy(AccessPolicy p, String parameter) { 
		this.policy = p;		
		this.policyParameter = parameter;
	}
	
	public AccessPolicy getAccessPolicy() { return policy; }	
	public String getPolicyParameter() { return policyParameter; }
}
