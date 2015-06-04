/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
 * (NIBIOHN), Japan.
 *
 * This file is part of Toxygates.
 *
 * Toxygates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Toxygates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Toxygates. If not, see <http://www.gnu.org/licenses/>.
 */

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
