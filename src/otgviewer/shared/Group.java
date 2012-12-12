package otgviewer.shared;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * A group of barcodes. Values will be computed as an average.
 * @author johan
 *
 */
public class Group implements Serializable, DataColumn, Comparable<Group> {

	private static final long serialVersionUID = 2111266740402283063L;
	Barcode[] barcodes;
	String name;
	
	public Group() {}
	
	public Group(String name, Barcode[] barcodes) {
		this.name = name;
		this.barcodes = barcodes;
	}
	
	public Barcode[] getBarcodes() { return barcodes; }
	public String getName() { return name; }
	
	public String toString() {
		return name;
	}
	
	public String getShortTitle() {
		return name;
	}
	
	public String[] getCompounds() {
		Set<String> compounds = new HashSet<String>();
		for (Barcode b : barcodes) {
			compounds.add(b.getCompound());
		}
		return compounds.toArray(new String[0]);		
	}
	
	public String pack() {
		StringBuilder s = new StringBuilder();
		s.append("Group:::");
		s.append(name + ":::"); //!!
		for (Barcode b : barcodes) {
			s.append(b.pack());
			s.append("^^^");
		}
		return s.toString();
	}
	
	public static Group unpack(String s) {
//		Window.alert(s + " as group");
		String[] s1 = s.split(":::"); // !!
		String name = s1[1];
		if (s1.length >= 3) {
			String[] s2 = s1[2].split("\\^\\^\\^");
			Barcode[] bcs = new Barcode[s2.length];			
			for (int i = 0; i < s2.length; ++i) {
				Barcode b = Barcode.unpack(s2[i]);
				bcs[i] = b;
			}
			return new Group(name, bcs);
		} else {
			return new Group(name, new Barcode[0]);
		}
	}
	
	public int compareTo(Group other) {
		return name.compareTo(other.getName());
	}
	
}
