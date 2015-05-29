package t.common.shared.clustering;

import java.util.ArrayList;
import java.util.List;

public enum Algorithm {
	HIERARCHICAL("Hierarchical", "Cut-off") {
		@Override
		public String[] getParam1Items() {
			return new String[] { "40", "80", "120" };
		}
		@Override
		public String[] getAllClusterings(String param1) {
			List<String> list = new ArrayList<String>();

			String[] classes = { "LN", "LV", "SP" };

			for (String c : classes) {
				list.add(c + "_K" + param1);
			}

			return list.toArray(new String[0]);
		}
		@Override
		public String[] getAllClusters(String param1, String clustering) {
			List<String> list = new ArrayList<String>();

			int k = Integer.parseInt(param1);

			for (int i = 1; i <= k;++i) {
				list.add(clustering + "_" + i);
			}

			return list.toArray(new String[0]);
		}
	};

	private String title; // Example: Hierarchical

	/**
	 * User-readable name of param1
	 */
	private String param1Name;

	private int numParams;

	private Algorithm(String title, String param1Name) {
		this.title = title;
		this.param1Name = param1Name;
		numParams = 1;
	}

	public String getTitle() {
		return title;
	}

	public String getParam1Name() {
		return param1Name;
	}

	public int getNumOfParams() {
		return numParams;
	}

	// Override this
	public String[] getParam1Items() {
		return null;
	}

	// Override this
	public String[] getAllClusterings(String param1) {
		return null;
	}

	// Override this
	public String[] getAllClusters(String param1, String clustering) {
		return null;
	}

}
