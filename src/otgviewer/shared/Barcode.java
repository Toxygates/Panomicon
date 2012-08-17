package otgviewer.shared;

import java.io.Serializable;

/**
 * A barcode corresponds to a single microarray.
 * @author johan
 *
 */
public class Barcode implements Serializable, DataColumn {
	private static final long serialVersionUID = -9107751439620262933L;
	
	private String code = "";	
	private String individual = "";
	private String dose = "";
	private String time = "";
	
	public Barcode() { }
	
	public Barcode(String _code, String _ind, 
			String _dose, String _time) {
		code = _code;		
		individual = _ind;
		dose = _dose;
		time = _time;			
	}
	
	public String getTitle() {
		return getShortTitle() + " (" + code + ")";
	}
	
	public String getShortTitle() {
		return dose + "/" + time + "/"+ individual;
	}
	
	public String getCode() {
		return code;
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
	
	public Barcode[] getBarcodes() { 
		return new Barcode[] { this };
	}
}
