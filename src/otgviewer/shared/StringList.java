package otgviewer.shared;

import java.util.Arrays;
import java.util.Collection;

public class StringList extends ItemList {

	private String[] items;
	
	/**
	 * This constructor is here for GWT serialization
	 */
	protected StringList() { }
	
	public StringList(String type, String name, String[] items) {
		super(type, name);
		this.items = items;
	}
	
	public Collection<String> packedItems() {
		return Arrays.asList(items);
	}
	
	public String[] items() { return items; }
	
	public int size() {
		if (items == null) {
			return 0;
		} else {
			return items.length;
		}
	}
	
}
