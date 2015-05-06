package t.viewer.shared;

import static otgviewer.client.components.StorageParser.packList;

import java.io.Serializable;
import java.util.Collection;

import t.common.shared.Packable;

/**
 * A typed, named list of items.
 * 
 * Current supported types are "probes" and "compounds".
 * However, lists of type probes may actually be gene identifiers (entrez).
 */
abstract public class ItemList implements Packable, Serializable {

	protected String type;
	protected String name;
	
	protected ItemList() { }
	
	public ItemList(String type, String name) {
		this.name = name;
		this.type = type;
	}
	
	public String name() { return name; }
	public String type() { return type; }
	
	public String pack() {
		StringBuilder sb = new StringBuilder();
		sb.append(type);
		sb.append(":::");
		sb.append(name);
		sb.append(":::");
		sb.append(packList(packedItems(), "^^^"));
		return sb.toString();
	}
	
	abstract protected Collection<String> packedItems();

	abstract public int size();
	
	public static ItemList unpack(String input) {
		if (input == null) {
			return null;
		}
		
		String[] spl = input.split(":::");
		if (spl.length < 3) {
			return null;
		}
		
		String type = spl[0];
		String name = spl[1];
		String[] items = spl[2].split("\\^\\^\\^");
	
		// TODO would be good to avoid having this kind of central registry
		// of list types here.
		if (type.equals("probes")) {
			return new StringList(type, name, items);
		} else if (type.equals("compounds")) {
			return new StringList(type, name, items);
		} else {
			// Unexpected type, ignore
			return null;
		} 
	}	
}
