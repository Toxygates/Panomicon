package otgviewer.shared;

import java.io.Serializable;

public class Synthetic implements DataColumn, Serializable {

	private static final long serialVersionUID = 1990541110612881170L;
	
	private String name;
	
	public Synthetic() { }
		
	public Synthetic(String name) { this.name = name; }

	public Barcode[] getBarcodes() { return new Barcode[0]; }
	
	public String[] getCompounds() { return new String[0]; }
	
	public String getShortTitle() { return name; }
	
	public String pack() {
		return "Synthetic:::" + name;
	}
}
