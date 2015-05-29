package t.common.shared.clustering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Clusterings {

	private static Map<String, Algorithm> algos = new HashMap<String, Algorithm>();

	public Clusterings() {
		for (Algorithm a : Algorithm.values()) {
			algos.put(a.getTitle(), a);
		}
	}

	public static Algorithm lookup(String name) {
		return algos.get(name);
	}

	public List<String> getNames() {
		return new ArrayList<String>(algos.keySet());
	}

	public String[] getAllClustering(String algorithmName, String param1) {
		return lookup(algorithmName).getAllClusterings(param1);
	}

	public String[] getAllClusters(String algorithmName, String param1,
			String clustering) {
		return lookup(algorithmName).getAllClusters(param1, clustering);
	}

}
