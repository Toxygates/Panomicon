package otgviewer.client.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import otgviewer.shared.Barcode;
import otgviewer.shared.BarcodeColumn;
import otgviewer.shared.CellType;
import otgviewer.shared.DataFilter;
import otgviewer.shared.Group;
import otgviewer.shared.Organ;
import otgviewer.shared.Organism;
import otgviewer.shared.RepeatType;
import bioweb.shared.array.DataColumn;

import com.google.gwt.storage.client.Storage;

/**
 * Eventually all storage parsing/serialising code should be centralised here,
 * but for now, some of it is still spread out in other classes, such as
 * Group and Barcode.
 */
public class StorageParser {

	private final String prefix;
	private final Storage storage;
	StorageParser(Storage storage, String prefix) {
		this.prefix = prefix;
		this.storage = storage;
	}
	
	void setItem(String key, String value) {
		storage.setItem(prefix + "." + key, value);
	}
	
	String getItem(String key) {
		return storage.getItem(prefix + "." + key);
	}
	
	void clearItem(String key) {
		storage.removeItem(key);
	}
	
	static String packDataFilter(DataFilter f) {
		return f.cellType.name() + "," + f.organ.name() + ","  
			+ f.repeatType.name() + "," + f.organism.name();
	}
	
	static DataFilter unpackDataFilter(String s) {
		if (s == null) {
			return null;
		}
		
		String[] parts = s.split(",");
		assert(parts.length == 4);
		
		try {
			DataFilter r = new DataFilter(CellType.valueOf(parts[0]),
					Organ.valueOf(parts[1]), RepeatType.valueOf(parts[2]),
					Organism.valueOf(parts[3]));			
			return r;
		} catch (Exception e) {			
			return null;
		}
	}
	
	static String packColumns(Collection<BarcodeColumn> columns) {
		List<String> cs = new ArrayList<String>();
		for (DataColumn<?> c : columns) {
			cs.add(c.pack());
		}
		return packList(cs, "###");
	}

	static BarcodeColumn unpackColumn(String s) {
		if (s == null) {
			return null;
		}
		String[] spl = s.split("\\$\\$\\$");
		if (spl[0].equals("Barcode")) {
			return Barcode.unpack(s);
		} else {
			return Group.unpack(s);
		}
	}
	
	static String packProbes(String[] probes) {
		return packList(Arrays.asList(probes), "###");
	}
	
	static String packList(Collection<String> items, String separator) {
		StringBuilder sb = new StringBuilder();
		for (String x: items) {
			sb.append(x);
			sb.append(separator);
		}
		return sb.toString();
	}
}
