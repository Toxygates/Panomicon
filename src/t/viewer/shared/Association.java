package t.viewer.shared;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import t.common.shared.AType;
import t.common.shared.Pair;

/**
 * An association is a mapping from probes to other objects. They are used as
 * "dynamic columns" in the GUI.
 * The mapped-to objects have names and formal identifiers.
 */
public class Association implements Serializable {

	private AType _type;
	private Map<String, ? extends Set<? extends Pair<String, String>>> _data = 
			new HashMap<String, HashSet<? extends Pair<String, String>>>();
	
	public Association() { }
	
	/**
	 * 
	 * @param type
	 * @param data Association data keyed on probe id:s.
	 * The first value in the value pair is the title, the second value is the formal identifier
	 */
	public Association(AType type, Map<String, ? extends Set<? extends Pair<String, String>>> data) {
		_type = type;
		_data = data;
	}
	
	public AType type() {
		return _type;
	}
	
	public String title() {
		return _type.name();
	}
	
	public Map<String, ? extends Set<? extends Pair<String, String>>> data() {
		return _data;
	}
}
