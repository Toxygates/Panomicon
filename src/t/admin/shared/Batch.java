package t.admin.shared;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import t.common.shared.ManagedItem;

public class Batch extends ManagedItem {

	private int numSamples;
	private Set<String> enabledInstances;
	private String dataset;
	
	public Batch() { }
	
	public Batch(String title, int numSamples, String comment,
			Date date, Set<String> enabledInstances,
			String dataset) {
		super(title, comment, date);		
		this.numSamples = numSamples;
		this.enabledInstances = enabledInstances;
		this.dataset = dataset;
	}

	public String getDataset() {
		return dataset;
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
	
	public void setDataset(String dataset) {
		this.dataset = dataset;
	}

}
