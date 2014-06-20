package t.admin.shared;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class Batch extends TitleItem {

	private int numSamples;
	private Set<String> enabledInstances;
	private Date date = new Date();
	private String comment = "Comment";
	
	public Batch() { }
	
	public Batch(String title, int numSamples, Set<String> enabledInstances) {
		super(title);		
		this.numSamples = numSamples;
		this.enabledInstances = enabledInstances;
	}
	
	public Batch(String title, int numSamples, String[] enabledInstances) {
		super(title);
		Set<String> enabled = new HashSet<String>();
		Collections.addAll(enabled,  enabledInstances);
		this.numSamples = numSamples;
		this.enabledInstances = enabled;		
	}

	public int getNumSamples() { return numSamples; }
	
	/**
	 * Get the list of instance IDs for which this batch is visible.
	 * @return
	 */
	public Set<String> getEnabledInstances() {
		return enabledInstances;
	}

	/**
	 * Set the list of instance IDs for which this batch is visible.
	 * @param enabled
	 */
	public void setEnabledInstanceTitles(Set<String> enabled) {
		this.enabledInstances = enabled;
	}
	
	public void setEnabledInstances(Set<Instance> enabled) {
		Set<String> en = new HashSet<String>();
		for (Instance i: enabled) {
			en.add(i.getTitle());
		}
		setEnabledInstanceTitles(en);
	}
	
	public String getComment() { return comment; }
	public void setComment(String c) { comment = c; }
	public Date getDate() { return date; }
}
