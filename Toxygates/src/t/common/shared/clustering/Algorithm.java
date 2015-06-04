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

public enum Algorithm {
	HIERARCHICAL("Hierarchical", "Cut-off") {
		@Override
		public String[] getParam1Items() {
			return new String[] { "40", "80", "120" };
		}
		@Override
		public String[] getAllClusterings(String param1) {
			List<String> list = new ArrayList<String>();

			for (String c : new String[]{ "LN", "LV", "SP" }) {
				list.add(c + "_K" + param1);
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
		throw new RuntimeException("Algorithm#getParam1Items is not implemented");
	}

	// Override this
	public String[] getAllClusterings(String param1) {
		throw new RuntimeException("Algorithm#getAllClusterings is not implemented");
	}

}
