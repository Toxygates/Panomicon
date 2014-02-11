package otgviewer.shared;

import static otgviewer.client.components.StorageParser.packList;

import java.util.Collection;

import bioweb.shared.Packable;

/**
 * TODO consider moving this class and its subclasses to otgviewer.client
 */
abstract public class ItemList implements Packable {

	protected final String name;
	protected final String type;
	public ItemList(String type, String name) {
		this.name = name;
		this.type = type;
	}
	
	public String name() { return name; }
	public String type() { return type; }
	
	public String pack() {
		StringBuilder sb = new StringBuilder();
		sb.append(name);
		sb.append(":::");
		sb.append(type);
		sb.append(":::");
		sb.append(packList(packedItems(), "^^^"));
		return sb.toString();
	}
	
	abstract protected Collection<String> packedItems();

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
		if (type.equals("genes")) {
			return new StringList(type, name, items);
		} else if (type.equals("compounds")) {
			return new StringList(type, name, items);
		} else {
			// Unexpected type, ignore
			return null;
		} 
	}	
}
