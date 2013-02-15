package otgviewer.shared;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Association implements Serializable {

	private AType _type;
	private Map<String, ? extends Set<Pair<String, String>>> _data = new HashMap<String, HashSet<Pair<String, String>>>();
	
	public Association() { }
	
	/**
	 * 
	 * @param type
	 * @param data The first value in pair is the title, the second value is the formal identifier
	 */
	public Association(AType type, Map<String, ? extends Set<Pair<String, String>>> data) {
		_type = type;
		_data = data;
	}
	
	public AType type() {
		return _type;
	}
	
	public String title() {
		return _type.name();
	}
	
	public Map<String, ? extends Set<Pair<String, String>>> data() {
		return _data;
	}
}
