package otgviewer.shared;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Association implements Serializable {

	private AType _type;
	private Map<String, ? extends Set<String>> _data = new HashMap<String, HashSet<String>>();
	
	public Association() { }
	
	public Association(AType type, Map<String, ? extends Set<String>> data) {
		_type = type;
		_data = data;
	}
	
	public AType type() {
		return _type;
	}
	
	public String title() {
		return _type.name();
	}
	
	public Map<String, ? extends Set<String>> data() {
		return _data;
	}
}
