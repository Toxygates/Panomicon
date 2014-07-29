package t.common.shared;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Information about the data schema in a particular T application.
 */
public abstract class DataSchema {

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
}
