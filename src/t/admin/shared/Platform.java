package t.admin.shared;

import java.util.Date;

import t.common.shared.ManagedItem;

public class Platform extends ManagedItem {

	private int numProbes;	
	
	public Platform() { }
	
	public Platform(String title, int numProbes, String comment, Date date) {
		super(title, comment, date);
		this.numProbes = numProbes;		
	}
	
	public int getNumProbes() {
		return numProbes;
	}
}
