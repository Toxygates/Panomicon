package otgviewer.shared;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class Association implements Serializable {

	private String _title;
	private Map<String, HashSet<String>> _data = new HashMap<String, HashSet<String>>();
	
	public Association() {
		
	}
	
	public Association(String title, Map<String, HashSet<String>> data) {
		_title = title;
		_data = data;
	}
	
	public String title() {
		return _title;
	}
	public Map<String, HashSet<String>> data() {
		return _data;
	}
}
