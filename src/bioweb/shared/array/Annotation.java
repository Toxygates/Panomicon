package bioweb.shared.array;

import java.io.Serializable;
import java.util.List;

/**
 * A set of annotations (such as chemical data and morphological data)
 * corresponding to a microarray sample.
 * @author johan
 */
public class Annotation implements Serializable {
	public Annotation(String id, List<Entry> annotations) {
		_id = id;
		_entries = annotations;
	}
	
	public Annotation() { }
	
	private String _id;	
	public String id() { return _id; }

	private List<Entry> _entries;
	
	public static class Entry implements Serializable {
		public String description;
		public String value;
		public boolean numerical;
		public Entry() {}
		public Entry(String description, String value, boolean numerical) {
			this.description = description;
			this.value = value;
			this.numerical = numerical;
		}
	}
	
	public List<Entry> getEntries() {

		return _entries;
	}
	
	public double doubleValueFor(String entryName) throws Exception {
		for (Annotation.Entry e: getEntries()) {
			if (e.description.equals(entryName)) {																										
				return Double.valueOf(e.value);
			}
		}
		throw new Exception("Value not available");
	}
}
