package otgviewer.shared;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * A group of barcodes. Values will be computed as an average.
 * @author johan
 *
 */
public class Group implements Serializable, DataColumn, Comparable<Group> {

	private static final long serialVersionUID = 2111266740402283063L;
	private static final String[] groupColours = new String[] { "DodgerBlue", "FireBrick", "DarkCyan", "Navy", 
		"LightSeaGreen", "PapayaWhip", "Purple", "DarkOliveGreen" };
	private static int nextColour = 0;
	
	Barcode[] barcodes;
	String name, colour;
	
	public Group() {}
	
	public Group(String name, Barcode[] barcodes, String colour) {
		this.name = name;
		this.barcodes = barcodes;
		this.colour = colour;
	}
	
	public Group(String name, Barcode[] barcodes) {
		this(name, barcodes, pickColour());
	}
	
	private static synchronized String pickColour() {
		nextColour += 1;
		if (nextColour > groupColours.length) {
			nextColour = 1;
		}
		return groupColours[nextColour - 1];
	}
	
	public Barcode[] getBarcodes() { return barcodes; }
	public String getName() { return name; }
	
	public String toString() {
		return name;
	}
	
	public String getShortTitle() {
		return name;
	}
	
	public String getColour() {
		return colour;
	}
	
	public String getCDTs(final int limit, String separator) {
		Set<String> CDTs = new HashSet<String>();
		Set<String> allCDTs = new HashSet<String>();
		for (Barcode b : barcodes) {			
			if (CDTs.size() < limit || limit == -1) {
				CDTs.add(b.getCDT());
			}
			allCDTs.add(b.getCDT());
		}
		String r = SharedUtils.mkString(CDTs, separator);
		if (allCDTs.size() > limit && limit != -1) {
			return r + "...";
		} else {
			return r;
		}
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
		s.append(colour + ":::");
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
		String colour = "";
		String barcodes = "";
		if (s1.length == 4) {
			colour = s1[2];
			barcodes = s1[3];
		} else if (s1.length == 3) {
			colour = pickColour();
			barcodes = s1[2];
		} else if (s1.length == 2) {
			colour = pickColour();
		}
		if (s1.length >= 3) {
			String[] s2 = barcodes.split("\\^\\^\\^");
			Barcode[] bcs = new Barcode[s2.length];			
			for (int i = 0; i < s2.length; ++i) {
				Barcode b = Barcode.unpack(s2[i]);
				bcs[i] = b;
			}
			return new Group(name, bcs, colour);
		} else {
			return new Group(name, new Barcode[0], colour);
		}
	}
	
	@Override
	public int compareTo(Group other) {
		return name.compareTo(other.getName());
	}
	
	@Override
	public int hashCode() {
		return Arrays.hashCode(barcodes) * 41 + name.hashCode();
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof Group) {
			return (Arrays.deepEquals(this.barcodes, ((Group) other).getBarcodes())) && name.equals(((Group) other).getName());
		}
		return false;
	}
	
}
