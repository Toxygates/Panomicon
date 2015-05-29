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
