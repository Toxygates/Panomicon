package otgviewer.shared;

import java.util.Arrays;
import java.util.Collection;

public class StringList extends ItemList {

	private final String[] items;
	public StringList(String type, String name, String[] items) {
		super(type, name);
		this.items = items;
	}
	
	public Collection<String> packedItems() {
		return Arrays.asList(items);
	}
	
	public String[] items() { return items; }
	
}
