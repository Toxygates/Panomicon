/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition 
 * (NIBIOHN), Japan.
 *
 * This file is part of Toxygates.
 *
 * Toxygates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Toxygates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Toxygates. If not, see <http://www.gnu.org/licenses/>.
 */

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
