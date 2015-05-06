package otgviewer.shared;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Data manipulation utility methods.
 * @author johan
 *
 */
public class GroupUtils {

	/**
	 * In the list of groups, find the one that has the given title.
	 * @param groups
	 * @param title
	 * @return
	 */
	public static Group findGroup(List<Group> groups, String title) {
		for (Group d : groups) {
			if (d.getName().equals(title)) {
				return d;
			}
		}
		return null;
	}

	/**
	 * In the list of groups, find the first one that contains the given sample.
	 * @param columns
	 * @param barcode
	 * @return
	 */
	public static Group groupFor(List<Group> columns, String barcode) {
		for (Group c : columns) {
			for (OTGSample b : c.getSamples()) {
				if (b.getCode().equals(barcode)) {
					return c;						
				}
			}
		}
		return null;
	}
	
	/**
	 * In the list of groups, find those that contain the given sample.
	 * @param columns
	 * @param barcode
	 * @return
	 */
	public static List<Group> groupsFor(List<Group> columns, String barcode) {
		List<Group> r = new ArrayList<Group>();
		for (Group c : columns) {
			for (OTGSample b : c.getSamples()) {
				if (b.getCode().equals(barcode)) {
					r.add(c);
					break;
				}
			}
		}
		return r;
	}

	public static Set<String> collect(List<Group> columns, String parameter) {
		Set<String> r = new HashSet<String>();
		for (Group g : columns) {
			for (String c : g.collect(parameter)) {			
				r.add(c);			
			}
		}
		return r;
	}
	
	/**
	 * Extract the Barcode object that has the given barcode (as a String)
	 * from the list of groups.
	 * @param columns
	 * @param barcode
	 * @return
	 */
	public static OTGSample barcodeFor(List<Group> columns, String barcode) {
		for (Group c : columns) {
			for (OTGSample b : c.getSamples()) {
				if (b.getCode().equals(barcode)) {
					return b;
				}
			}
		}
		return null;
	}
	
	
}
