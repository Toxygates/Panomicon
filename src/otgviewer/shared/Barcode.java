package otgviewer.shared;

import bioweb.shared.array.Sample;

/**
 * A barcode corresponds to a single microarray.
 * @author johan
 *
 */
public class Barcode extends Sample implements BarcodeColumn {
		
	private String individual = "";
	private String dose = "";
	private String time = "";
	private String compound = "";
	
	public Barcode() { super(); }
	
	public Barcode(String _code, String _ind, 
			String _dose, String _time, String _compound) {
		super(_code);		
		individual = _ind;
		dose = _dose;
		time = _time;		
		compound = _compound;
	}
	
	public String getTitle() {
		return getShortTitle() + " (" + id() + ")";
	}
	
	public String getShortTitle() {
		return dose + "/" + time + "/"+ individual;
	}
	
	public String getCDT() {
		return compound + "/" + dose + "/" + time;
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
	
	public Barcode[] getBarcodes() { 
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
