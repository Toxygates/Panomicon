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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import t.viewer.shared.ItemList;
import t.viewer.shared.StringList;

public class ProbeClustering {
	private Algorithm algorithm; // example: Hierarchical
	private String param1; // Example: 120
	private String name; // example: LV_K120
	private List<ItemList> clusters;

	public ProbeClustering(Algorithm algorithm, String param1, String name,
			List<ItemList> clusters) {
		this.algorithm = algorithm;
		this.param1 = param1;
		this.name = name;
		this.clusters = clusters;
	}

	// TODO should use sparql query
	public static List<ProbeClustering> createFrom(
			Collection<StringList> clusters) {
		Map<String, ProbeClustering> m = new HashMap<String, ProbeClustering>();

		for (StringList c : clusters) {
			String title = getTitle(c.name());
			if (!m.containsKey(title)) {
				m.put(title, new ProbeClustering(Algorithm.HIERARCHICAL,
						getParam(title), title, new LinkedList<ItemList>()));
			}

			ProbeClustering p = m.get(title);
			p.clusters.add(c);
		}

		return new ArrayList<ProbeClustering>(m.values());
	}

	// TODO improve robustness
	private static String getTitle(String name) {
		int i = name.lastIndexOf("_C");
		if (i == -1) {
			throw new IllegalArgumentException("unknow format: " + name);
		}
		return name.substring(0, i);
	}

	// TODO improve robustness
	private static String getParam(String title) {
		int i = title.indexOf("_K");
		if (i == -1) {
			throw new IllegalArgumentException("unknow format: " + title);
		}
		return title.substring(i + 2);
	}

	public Algorithm getAlgorithm() {
		return algorithm;
	}

	public String getParam1() {
		return param1;
	}

	public String getName() {
		return name;
	}

	// Suggested API
	// TODO future work: fetch cluster by sparql query
	public List<ItemList> getClusters() {
		return clusters;
	}

	public static List<ProbeClustering> filterByAlgorithm(
			List<ProbeClustering> from, String algorithm) {
		List<ProbeClustering> l = new ArrayList<ProbeClustering>();

		for (ProbeClustering pc : from) {
			if (pc.getAlgorithm().getTitle().equals(algorithm)) {
				l.add(pc);
			}
		}

		return l;
	}

	public static List<ProbeClustering> filterByParam1(
			List<ProbeClustering> from, String param1) {
		List<ProbeClustering> l = new ArrayList<ProbeClustering>();

		for (ProbeClustering pc : from) {
			if (pc.getParam1().equals(param1)) {
				l.add(pc);
			}
		}

		return l;
	}

	public static List<ProbeClustering> filterByName(
			List<ProbeClustering> from, String name) {
		List<ProbeClustering> l = new ArrayList<ProbeClustering>();

		for (ProbeClustering pc : from) {
			if (pc.getName().equals(name)) {
				l.add(pc);
			}
		}

		return l;
	}

	public static Set<String> collectAlgorithm(List<ProbeClustering> from) {
		Set<String> s = new HashSet<String>();

		for (ProbeClustering p : from) {
			Algorithm algo = p.getAlgorithm();
			if (algo != null) {
				s.add(algo.getTitle());
			}
		}

		return s;
	}

	public static Set<String> collectParam1(List<ProbeClustering> from) {
		Set<String> s = new HashSet<String>();

		for (ProbeClustering p : from) {
			String param = p.getParam1();
			if (param != null) {
				s.add(param);
			}
		}

		return s;
	}

	public static Set<String> collectName(List<ProbeClustering> from) {
		Set<String> s = new HashSet<String>();

		for (ProbeClustering p : from) {
			String name = p.getName();
			if (name != null) {
				s.add(name);
			}
		}

		return s;
	}

}
