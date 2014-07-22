package otgviewer.shared;

import javax.annotation.Nullable;

import t.common.shared.sample.Sample;
import t.viewer.shared.SampleClass;

/**
 * A barcode corresponds to a single microarray sample.
 * 
 * TODO: rename, Barcode is not a good name
 */
public class Barcode extends Sample implements BarcodeColumn {
		
	private String individual = "";
	private String dose = "";
	private String time = "";
	private String compound = "";
	private BUnit unit;
	
	public Barcode() { super(); }
	
	public Barcode(String _code, String _ind, 
			String _dose, String _time, String _compound,
			@Nullable DataFilter filter) {
		super(_code);		
		individual = _ind;
		dose = _dose;
		time = _time;		
		compound = _compound;
		unit = new BUnit(this, SampleClass.fromDataFilter(filter));
	}
	
	public String getTitle() {
		return getShortTitle() + " (" + id() + ")";
	}
	
	public String getShortTitle() {
		return dose + "/" + time + "/"+ individual;
	}
	
	/**
	 * Obtain a short string that identifies the compound/dose/time combination
	 * for this sample.
	 * @return
	 */
	public String getParamString() {
		return unit.toString();
	}
	
	public BUnit getUnit() {
		return unit;
	}
	
	public String getCode() {
		return id();
	}
	
	public String getIndividual() {
		return individual;
	}
	
	public String getDose() {
		return dose;
	}
	
	public String getTime() {
		return time;
	}
	
	public String toString() {
		return getShortTitle();
	}
	
	public String getCompound() { 
		return compound;
	}
	
	public Barcode[] getSamples() { 
		return new Barcode[] { this };
	}
	
	public String[] getCompounds() {
		return new String[] { compound };
	}
	
	public static Barcode unpack(String s) {
//		Window.alert(s + " as barcode");
		String[] s1 = s.split("\\$\\$\\$");
		
		if (s1.length == 6) {		
			//Version 1
			return new Barcode(s1[1], s1[2], s1[3], s1[4], s1[5], null);
		} else if (s1.length == 10) {
			//Version 2
			return new Barcode(s1[1], s1[2], s1[3], s1[4], s1[5],
					new DataFilter(CellType.valueOf(s1[6]),
							Organ.valueOf(s1[7]), RepeatType.valueOf(s1[8]),
							Organism.valueOf(s1[9]))
			);			
		} else {			
			return null;
		}
	}
	
	public String pack() {
		final String sep = "$$$";
		StringBuilder sb = new StringBuilder();
		sb.append("Barcode").append(sep);
		sb.append(id()).append(sep);
		sb.append(individual).append(sep);
		sb.append(dose).append(sep);
		sb.append(time).append(sep);
		sb.append(compound).append(sep);
		if (unit.getCellType() != null) {
			sb.append(unit.getCellType().name()).append(sep);
			sb.append(unit.getOrgan().name()).append(sep);
			sb.append(unit.getRepeatType().name()).append(sep);
			sb.append(unit.getOrganism().name()).append(sep);
		}
		return sb.toString();
	}

}
