package otgviewer.shared;

import java.util.HashSet;
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
}
