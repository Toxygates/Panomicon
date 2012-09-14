package otgviewer.shared;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * A group of barcodes. Values will be computed as an average.
 * @author johan
 *
 */
public class Group implements Serializable, DataColumn {

	private static final long serialVersionUID = 2111266740402283063L;
	Barcode[] barcodes;
	String name;
	
	public Group() {}
	
	public Group(String name, Barcode[] barcodes) {
		this.name = name;
		this.barcodes = barcodes;
	}
	
	public Barcode[] getBarcodes() { return barcodes; }
	public String getName() { return name; }
	
	public String toString() {
		return name;
	}
	
	public String getShortTitle() {
		return name;
	}
	
	public String[] getCompounds() {
		Set<String> compounds = new HashSet<String>();
		for (Barcode b : barcodes) {
			compounds.add(b.getCompound());
		}
		return compounds.toArray(new String[0]);		
	}
}
