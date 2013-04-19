package otgviewer.shared;

import java.util.HashSet;
import java.util.Set;

import bioweb.shared.SharedUtils;
import bioweb.shared.array.SampleGroup;

/**
 * A group of barcodes. Values will be computed as an average.
 * @author johan
 *
 */
public class Group extends SampleGroup<Barcode> implements BarcodeColumn {
	
	public Group() {}
	
	public Group(String name, Barcode[] barcodes, String color) {
		super(name, barcodes, color);		
	}
	
	public Group(String name, Barcode[] barcodes) { super(name, barcodes); }

	public String getShortTitle() {
		return name;
	}

	public Barcode[] getBarcodes() { return _samples; }
	
	public String getCDTs(final int limit, String separator) {
		Set<String> CDTs = new HashSet<String>();
		Set<String> allCDTs = new HashSet<String>();
		for (Barcode b : _samples) {			
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
		for (Barcode b : _samples) {
			compounds.add(b.getCompound());
		}
		return compounds.toArray(new String[0]);		
	}
	
	public static Group unpack(String s) {
//		Window.alert(s + " as group");
		String[] s1 = s.split(":::"); // !!
		String name = s1[1];
		String color = "";
		String barcodes = "";
		if (s1.length == 4) {
			color = s1[2];
			barcodes = s1[3];
			if (SharedUtils.indexOf(groupColors, color) == -1) {
				//replace the color if it is invalid.
				//this lets us safely upgrade colors in the future.
				color = groupColors[0]; 
			}
		} else if (s1.length == 3) {
			color = groupColors[0];
			barcodes = s1[2];
		} else if (s1.length == 2) {
			color = groupColors[0];
		}
		if (s1.length >= 3) {
			String[] s2 = barcodes.split("\\^\\^\\^");
			Barcode[] bcs = new Barcode[s2.length];			
			for (int i = 0; i < s2.length; ++i) {
				Barcode b = Barcode.unpack(s2[i]);
				bcs[i] = b;
			}
			return new Group(name, bcs, color);
		} else {
			return new Group(name, new Barcode[0], color);
		}
	}

}
