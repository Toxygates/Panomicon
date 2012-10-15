package otgviewer.shared;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class Association implements Serializable {

	private String title;
	private Map<String, HashSet<String>> data = new HashMap<String, HashSet<String>>();
	
	public Association() {
		
	}
	
	public Association(String title, Map<String, HashSet<String>> data) {
		this.title = title;
		this.data = data;
	}
	
	public String getTitle() {
		return title;
	}
	public Map<String, HashSet<String>> getData() {
		return data;
	}
}
