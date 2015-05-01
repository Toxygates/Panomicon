package t.viewer.shared;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;

import javax.annotation.Nullable;

import t.common.shared.AType;
import t.common.shared.HasClass;
import t.common.shared.SampleClass;
import t.common.shared.SharedUtils;
import t.common.shared.ValueType;

/**
 * Information about the data schema in a particular T application.
 * 
 * TODO: move to t.common when the dependency on Unit has been resolved
 */
public abstract class DataSchema implements Serializable {	
	
	/**
	 * All the possible values, in their natural order, for a sortable
	 * parameter.
	 */
	public abstract String[] sortedValues(String parameter) throws Exception;
	
	/**
	 * Ordered values for a sortable parameter, which should be displayed to
	 * the user in the context of a given list of sample classes.
	 */
	//TODO move ValueType or avoid depending on here
	public String[] sortedValuesForDisplay(@Nullable ValueType vt, 
			String parameter) throws Exception {
		return filterValuesForDisplay(vt, parameter, sortedValues(parameter));		
	}
	
	//TODO move ValueType or avoid depending on here
	public String[] filterValuesForDisplay(@Nullable ValueType vt, 
			String parameter, String[] from) {
		return from;
	}
	
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
	
	public boolean isControlValue(String value) {
		return false;
	}
	
	public Unit selectionControlUnitFor(Unit u) {
		return null;
	}
	
	public boolean isMajorParamSharedControl(String value) {
		return false;
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
	
	public String platformSpecies(String platform) { 
		return platform.substring(0, 3) + "..";		
	}
	
	public abstract int numDataPointsInSeries(SampleClass sc);
	
	//TODO move down to otg
	@Nullable public String organismPlatform(String organism) {
		return null;
	}
	
}
