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

package otgviewer.client.components;

import java.util.List;

import otgviewer.shared.Group;
import otgviewer.shared.OTGColumn;
import t.common.shared.Dataset;
import t.common.shared.ItemList;
import t.common.shared.SampleClass;

public interface DataViewListener {
	public void datasetsChanged(Dataset[] ds);
	
	public void sampleClassChanged(SampleClass sc);
	
	public void probesChanged(String[] probes);
	
	public void compoundChanged(String compound);
	
	public void compoundsChanged(List<String> compounds);
	
	public void availableCompoundsChanged(List<String> compounds);
	
	public void columnsChanged(List<Group> columns);
	
	public void customColumnChanged(OTGColumn column);
	
	public void itemListsChanged(List<ItemList> lists);
	
}
