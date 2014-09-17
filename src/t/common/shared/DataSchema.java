package t.common.shared;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;

import otgviewer.shared.Group;
import t.common.shared.probe.ProbeCombiner;
import t.viewer.shared.AType;

/**
 * Information about the data schema in a particular T application.
 */
public abstract class DataSchema implements Serializable {	
	
	/**
	 * All the possible values, in their natural order, for a sortable
	 * parameter.
	 */
	public abstract String[] sortedValues(String parameter) throws Exception;
	
	/**
	 * Sort values from the given sortable parameter in place.
	 */
	public void sort(String parameter, String[] values) throws Exception {
		final String[] sorted = sortedValues(parameter);
		Arrays.sort(values, new Comparator<String>() {
			public int compare(String e1, String e2) {
				Integer i1 = SharedUtils.indexOf(sorted, e1);
				Integer i2 = SharedUtils.indexOf(sorted, e2);
				return i1.compareTo(i2);
			}
		});
	}
	
	public void sortTimes(String[] times) throws Exception {		
		sort(timeParameter(), times);		
	}
	
	/**
	 * Used in the "compound list", and other places
	 * @return
	 */
	public abstract String majorParameter();
	
	/**
	 * Used for columns in the time/dose selection grid
	 * @return
	 */
	public abstract String mediumParameter();
	
	/**
	 * Used for subcolumns (checkboxes) in the time/dose selection grid
	 * @return
	 */
	public abstract String minorParameter();
	
	/**
	 * Used for charts
	 * @return
	 */
	public abstract String timeParameter();
	
	/**
	 * Used to group charts in columns, when possible
	 * @return
	 */
	public abstract String timeGroupParameter();
	
	public abstract String[] macroParameters();
	
	/**
	 * Human-readable title
	 * @param parameter
	 * @return
	 */
	public abstract String title(String parameter);
	
	public boolean isSelectionControl(SampleClass sc) {
		return false;
	}
	
	public boolean isControlParameter(String value) {
		return false;
	}
	
	public Unit selectionControlUnitFor(Unit u) {
		return null;
	}
	
	/**
	 * The value of the major parameter that corresponds to a shared
	 * control unit, if any.
	 */
	@Nullable
	public String majorParamSharedControlValue() {
		return null;
	}
	
	public AType[] associations() { return new AType[] {}; }
	
	public String getMinor(HasClass hc) {
		return hc.sampleClass().get(minorParameter());
	}
	
	public String getMedium(HasClass hc) {
		return hc.sampleClass().get(mediumParameter());
	}
	
	public String getMajor(HasClass hc) {
		return hc.sampleClass().get(majorParameter());
	}
	
	@Nullable
	/**
	 * Obtain the probe combiner to be applied for a given set of groups.
	 * @param groups
	 * @return
	 */
	public ProbeCombiner probeCombiner(List<Group> groups) { return null; }
}
