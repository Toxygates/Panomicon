package otgviewer.shared;

import bioweb.shared.array.Sample;

/**
 * A barcode corresponds to a single microarray sample.
 * @author johan
 *
 */
public class Barcode extends Sample implements BarcodeColumn {
		
	private String individual = "";
	private String dose = "";
	private String time = "";
	private String compound = "";
	private BUnit unit;
	
	public Barcode() { super(); }
	
	public Barcode(String _code, String _ind, 
			String _dose, String _time, String _compound) {
		super(_code);		
		individual = _ind;
		dose = _dose;
		time = _time;		
		compound = _compound;
		unit = new BUnit(compound, dose, time);
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
	public String getCDT() {
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
		return new Barcode(s1[1], s1[2], s1[3], s1[4], s1[5]);
	}
	
	public String pack() {
		return "Barcode$$$" + id() + "$$$" + individual + "$$$" + dose + "$$$" + time + "$$$" + compound; //!!
	}

}
