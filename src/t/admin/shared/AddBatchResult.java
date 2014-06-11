package t.admin.shared;

import java.io.Serializable;

/**
 * The result of adding a batch.
 * @author johan
 */
public class AddBatchResult implements Serializable {
	private int numSamples;
	private String[] newSpecies, newTissues, newDoses, newTimes;
	
	int numSamples() { return numSamples; }
	
	String[] newSpecies() { return newSpecies; }
	
	String[] newTissues() { return newTissues; }
	
	String[] newDoses() { return newDoses; }
	
	String[] newTimes() { return newTimes; }
}
