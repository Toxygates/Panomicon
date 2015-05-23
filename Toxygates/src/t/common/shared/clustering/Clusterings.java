package t.common.shared.clustering;

import java.util.HashMap;
import java.util.Map;

public class Clusterings {

	private static Map<String, Algorithm> algos = new HashMap<String, Algorithm>();
	
	public static Algorithm lookup(String name) {
		return algos.get(name);
	}
}
