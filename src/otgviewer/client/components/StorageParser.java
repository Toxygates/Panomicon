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
import otgviewer.shared.ItemList;
import otgviewer.shared.Organ;
import otgviewer.shared.Organism;
import otgviewer.shared.RepeatType;
import bioweb.shared.Packable;

import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.Window;

/**
 * Eventually all storage parsing/serialising code should be centralised here,
 * but for now, some of it is still spread out in other classes, such as
 * Group and Barcode.
 */
public class StorageParser {

	private final String prefix;
	private final Storage storage;
	private static final char [] reservedChars = new char[] { ':', '#', '$', '^' };
	public static final String unacceptableStringMessage = 
			"The characters ':', '#', '$' and '^' are reserved and may not be used.";
	
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
	
	public static String packDataFilter(DataFilter f) {
		return f.cellType.name() + "," + f.organ.name() + ","  
			+ f.repeatType.name() + "," + f.organism.name();
	}
	
	public static DataFilter unpackDataFilter(String s) {
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
	
	public static String packColumns(Collection<BarcodeColumn> columns) {
		return packPackableList(columns, "###");
	}

	public static BarcodeColumn unpackColumn(String s) {
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
	
	public static String packProbes(String[] probes) {
		return packList(Arrays.asList(probes), "###");
	}
	
	public static String packPackableList(Collection<? extends Packable> items, String separator) {
		List<String> xs = new ArrayList<String>();
		for (Packable p: items) {
			xs.add(p.pack());
		}
		return packList(xs, separator);
	}
	
	public static String packList(Collection<String> items, String separator) {
		StringBuilder sb = new StringBuilder();
		for (String x: items) {
			sb.append(x);
			sb.append(separator);
		}
		return sb.toString();
	}
	
	public static String packItemLists(Collection<ItemList> lists, String separator) {
		return packPackableList(lists, separator);		
	}
	
	public static boolean isAcceptableString(String test, String failMessage) {
		for (char c: reservedChars) {
			if (test.indexOf(c) != -1) {
				Window.alert(failMessage + " " + unacceptableStringMessage);
				return false;
			}
		}
		return true;
	}	

}
