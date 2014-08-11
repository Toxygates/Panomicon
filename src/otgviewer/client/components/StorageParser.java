package otgviewer.client.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import otgviewer.client.Utils;
import otgviewer.shared.Group;
import otgviewer.shared.OTGColumn;
import t.common.shared.DataSchema;
import t.common.shared.Packable;
import t.viewer.shared.ItemList;

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
	
	protected static final Logger logger = Utils.getLogger("storage");
	
	StorageParser(Storage storage, String prefix) {
		this.prefix = prefix;
		this.storage = storage;
	}
	
	void setItem(String key, String value) {
		storage.setItem(prefix + "." + key, value);
//		logger.info("SET " + key + " -> " + value);
	}
	
	String getItem(String key) {
		String v = storage.getItem(prefix + "." + key);
//		logger.info("GET " + key + " -> " + v);
		return v;
		
	}
	
	void clearItem(String key) {
		storage.removeItem(key);
	}
	
	public static String packColumns(Collection<? extends OTGColumn> columns) {
		return packPackableList(columns, "###");
	}

	@Nullable
	public static Group unpackColumn(DataSchema schema, String s) {
		if (s == null) {
			return null;
		}				
		String[] spl = s.split("\\$\\$\\$");
		if (!spl[0].equals("Barcode") && !spl[0].equals("Barcode_v3")) {			
			return Group.unpack(schema, s);
		} else {
			//Legacy or incorrect format
			logger.warning("Unexpected column format: " + s);
			return null;
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
