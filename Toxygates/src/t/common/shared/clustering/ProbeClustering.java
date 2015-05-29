package t.common.shared.clustering;

import java.util.ArrayList;
import java.util.List;

import t.viewer.shared.StringList;

public class ProbeClustering {
	private Algorithm algorithm; // example: Hierarchical
	private String param1; // Example: 120
	private String name; // example: LV_K120

	public ProbeClustering(String name, Algorithm algorithm, String param1) {
		this.name = name;
		this.algorithm = algorithm;
		this.param1 = param1;
	}

	public String getParam1() {
		return param1;
	}

	// Suggested API
	public List<StringList> getClusters() {
		return new ArrayList<StringList>();
	}

}
