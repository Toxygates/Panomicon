package otgviewer.shared;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Association implements Serializable {

	private String _title;
	private Map<String, ? extends Set<String>> _data = new HashMap<String, HashSet<String>>();
	
	public Association() {
		
	}
	
	public Association(String title, Map<String, ? extends Set<String>> data) {
		_title = title;
		_data = data;
	}
	
	public String title() {
		return _title;
	}
	public Map<String, ? extends Set<String>> data() {
		return _data;
	}
}
