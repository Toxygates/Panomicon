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

package otgviewer.client.charts.google;

import otgviewer.client.charts.Data;

import com.google.gwt.visualization.client.DataTable;

public class GDTData extends Data {

	private DataTable data;
	
	public GDTData(DataTable data) {
		this.data = data;
	}

	@Override
	public int numberOfColumns() {
		// TODO Auto-generated method stub
		return data.getNumberOfColumns();
	}
	
	DataTable data() { return data; }
}
