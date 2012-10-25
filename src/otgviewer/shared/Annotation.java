package otgviewer.shared;

import java.io.Serializable;
import java.util.List;

/**
 * A set of annotations (such as chemical data and morphological data)
 * corresponding to an array.
 * @author johan
 */
public class Annotation implements Serializable {
	public Annotation(String barcode, List<Entry> annotations) {
		_barcode = barcode;
		_entries = annotations;
	}
	
	public Annotation() { }
	
	private String _barcode;	
	public String barcode() { return _barcode; }
//	
//	private Map<String, String> _annotations;
//	public Map<String, String> annotations() { return _annotations; }
	
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
//		List<Entry> r = new ArrayList<Entry>();
//		for (String k: _annotations.keySet()) {
//			r.add(new Entry(k, _annotations.get(k)));
//		}
//		return r;
		return _entries;
	}
}
