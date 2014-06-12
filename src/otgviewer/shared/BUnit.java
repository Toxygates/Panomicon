package otgviewer.shared;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import bioweb.shared.array.Unit;

public class BUnit extends Unit<Barcode> {

	protected BUnit() { super(); }
	private String _time, _dose, _compound;
	
	public BUnit(String compound, String dose, String time) {
		super(compound + "/" + dose + "/" + time);
		_time = time;
		_dose = dose;
		_compound = compound;
	}
	
	public BUnit(Barcode b) {
		this(b.getCompound(), b.getDose(), b.getTime());
	}
	
	public String getTime() {
		return _time;
	}
	
	public String getDose() {
		return _dose;
	}
	
	public String getCompound() {
		return _compound;
	}
	
	@Override 
	public int hashCode() {
		return _time.hashCode() + _dose.hashCode() + _compound.hashCode();
	}
	
	@Override
	public boolean equals(Object other) {
		if ((other instanceof BUnit)) {
			BUnit that = (BUnit) other;
			return _time.equals(that.getTime()) && 
					_dose.equals(that.getDose()) && 
					_compound.equals(that.getCompound());
		} else {
			return false;
		}
	}
	
	public static String[] compounds(BUnit[] units) {
		Set<String> r = new HashSet<String>();
		for (BUnit b : units) {
			r.add(b.getCompound());
		}
		return r.toArray(new String[0]);
	}
	
	public static String[] times(BUnit[] units) {
		Set<String> r = new HashSet<String>();
		for (BUnit b : units) {
			r.add(b.getTime());
		}
		return r.toArray(new String[0]);
	}
	
	public static String[] doses(BUnit[] units) {
		Set<String> r = new HashSet<String>();
		for (BUnit b : units) {
			r.add(b.getDose());
		}
		return r.toArray(new String[0]);
	}
	
	public static boolean containsTime(BUnit[] units, String time) {
		Set<String> r = new HashSet<String>();
		for (BUnit b : units) {
			r.add(b.getTime());
		}
		return r.contains(time);
	}
	
	public static boolean containsDose(BUnit[] units, String dose) {
		Set<String> r = new HashSet<String>();
		for (BUnit b : units) {
			r.add(b.getDose());
		}
		return r.contains(dose);
	}
	
	public static BUnit[] formUnits(Barcode[] barcodes) {
		Map<String, List<Barcode>> units = new HashMap<String, List<Barcode>>();
		for (Barcode b: barcodes) {
			String cdt = b.getCDT();
			if (units.containsKey(cdt)) {
				units.get(cdt).add(b);
			} else {
				List<Barcode> n = new ArrayList<Barcode>();
				n.add(b);
				units.put(cdt, n);
			}
		}
		ArrayList<BUnit> r = new ArrayList<BUnit>();
		for (List<Barcode> bcs: units.values()) {
			BUnit b = new BUnit(bcs.get(0));
			b.setSamples(bcs.toArray(new Barcode[0]));
			r.add(b);
		}
		return r.toArray(new BUnit[0]);
	}
	
	public static Barcode[] collectBarcodes(BUnit[] units) {
		List<Barcode> r = new ArrayList<Barcode>();
		for (BUnit b: units) {
			Collections.addAll(r, b.getSamples());		
		}
		return r.toArray(new Barcode[0]);
	}
}
