package otgviewer.shared;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import t.common.shared.sample.Unit;
import t.viewer.shared.SampleClass;

/**
 * A BUnit is a Unit of Barcodes.
 */
public class BUnit extends Unit<Barcode> {

	protected BUnit() { super(); }
	private String _time, _dose, _compound;
	
	//TODO consider extracting an interface SampleParameter and lifting up some of these
	//TODO think about how these should be preserved across the various transformation
	//methods. Should ideally store in Barcode too.
	private Organ _organ = null;
	private Organism _organism = null;
	private CellType _cellType = null;
	private RepeatType _repeatType = null;
	
	public BUnit(String compound, String dose, String time) {
		super(compound + "/" + dose + "/" + time);
		_time = time;
		_dose = dose;
		_compound = compound;
	}
	
	public BUnit(Barcode b, @Nullable SampleClass sc) {
		this(b.getCompound(), b.getDose(), b.getTime());
		if (sc != null) {
			setSampleClass(sc);
		}
	}
	
	public void setSampleClass(SampleClass sc) {
		DataFilter filter = sc.asDataFilter();
	
		_organ = filter.organ;
		_organism = filter.organism;
		_cellType = filter.cellType;
		_repeatType = filter.repeatType;

		_name = _organ + "/" + _organism + "/" + _cellType + "/" + _repeatType
				+ "/" + _name;
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
	
	@Nullable
	public Organ getOrgan() {
		return _organ;
	}
	
	@Nullable
	public Organism getOrganism() {
		return _organism;
	}
	
	@Nullable
	public CellType getCellType() {
		return _cellType;
	}
	
	@Nullable
	public RepeatType getRepeatType() {
		return _repeatType;
	}
	
	@Override 
	public int hashCode() {
		int r = _time.hashCode() + _dose.hashCode() + _compound.hashCode();
		if (_organ != null) {
			r = r + _organ.hashCode() + _organism.hashCode() + _repeatType.hashCode() +
					_cellType.hashCode();
		}
		return r;
	}
	
	@Override
	public boolean equals(Object other) {
		if ((other instanceof BUnit)) {
			BUnit that = (BUnit) other;
			
			//TODO in the future, organism, organ etc should never be null
			
			return _time.equals(that.getTime()) && _dose.equals(that.getDose())
					&& _compound.equals(that.getCompound())
					&& safeCompare(_organ, that.getOrgan())
					&& safeCompare(_organism, that.getOrganism())
					&& safeCompare(_repeatType, that.getRepeatType())
					&& safeCompare(_cellType, that.getCellType());
							
		} else {
			return false;
		}
	}
	
	private static boolean safeCompare(Object v1, Object v2) {
		if (v1 == null && v2 == null) {
			return true;
		} else if (v1 != null && v2 != null) {
			return v1.equals(v2);
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
	
	public static BUnit[] formUnits(Barcode[] barcodes, SampleClass sc) {
		Map<String, List<Barcode>> units = new HashMap<String, List<Barcode>>();
		for (Barcode b: barcodes) {
			String cdt = b.getParamString();
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
			Barcode first = bcs.get(0);
			BUnit b = (first.getUnit().getOrgan() == null) ? 
					new BUnit(bcs.get(0), sc) : first.getUnit(); 
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
}
