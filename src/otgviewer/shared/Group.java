package otgviewer.shared;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import bioweb.shared.SharedUtils;
import bioweb.shared.array.SampleGroup;

/**
 * A group of barcodes. Values will be computed as an average.
 * @author johan
 *
 */
public class Group extends SampleGroup<Barcode> implements BarcodeColumn {
	
	protected BUnit[] _units;
	
	public Group() {}
	
	public Group(String name, Barcode[] barcodes, String color, @Nullable DataFilter filter) {
		super(name, barcodes, color);	
		_units = BUnit.formUnits(barcodes, filter);
	}
	
	public Group(String name, Barcode[] barcodes, @Nullable DataFilter filter) { 
		super(name, barcodes); 
		_units = BUnit.formUnits(barcodes, filter);
	}
	
	public Group(String name, BUnit[] units) { 
		super(name, BUnit.collectBarcodes(units)); 
		_units = units;
	}
	
	public Group(String name, BUnit[] units, String color, @Nullable DataFilter filter) {
		this(name, BUnit.collectBarcodes(units), color, filter);
	}

	public String getShortTitle() {
		return name;
	}

	public Barcode[] getSamples() { return _samples; }
	
	public Barcode[] getTreatedSamples() {
		List<Barcode> r = new ArrayList<Barcode>();
		for (BUnit u : _units) {
			if (!u.getDose().equals("Control")) {
				r.addAll(Arrays.asList(u.getSamples()));
			}
		}
		return r.toArray(new Barcode[0]);
	}
	
	public Barcode[] getControlSamples() {
		List<Barcode> r = new ArrayList<Barcode>();
		for (BUnit u : _units) {
			if (u.getDose().equals("Control")) {
				r.addAll(Arrays.asList(u.getSamples()));
			}
		}
		return r.toArray(new Barcode[0]);
	}
	
	public BUnit[] getUnits() { return _units; }
	
	public String getCDTs(final int limit, String separator) {
		Set<String> CDTs = new HashSet<String>();
		boolean stopped = false;
		for (BUnit u : _units) {
			if (u.getDose().equals("Control")) {
				continue;
			}
			if (CDTs.size() < limit || limit == -1) {
				CDTs.add(u.toString());
			} else {
				stopped = true;
				break;
			}
		}
		String r = SharedUtils.mkString(CDTs, separator);
		if (stopped) {
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
	
	// See SampleGroup for the packing method
	// TODO lift up the unpacking code to have 
	// the mirror images in the same class, if possible
	public static Group unpack(String s, DataFilter filter) {
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
			DataFilter useFilter = (bcs[0].getUnit().getOrgan() == null) ? filter : null;
			return new Group(name, bcs, color, useFilter);
			
		} else {
			return new Group(name, new Barcode[0], color, null);
		}
	}

}
