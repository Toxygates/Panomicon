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

package t.admin.client;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import t.admin.shared.Batch;
import t.admin.shared.Instance;
import t.common.client.DataRecordSelector;

/**
 * GUI for editing the visibility of a batch.
 * @author johan
 */
public class VisibilityEditor extends DataRecordSelector<Instance> {

	static final String message =
			"Please select the instances in which this batch should be\n visible. Changes are saved immediately.";
	
	public VisibilityEditor(Batch batch, Collection<Instance> instances) {
		super(instances, message);
		
		Set<Instance> initSel = new HashSet<Instance>();
		for (Instance i: instances) {
			if (batch.getEnabledInstances().contains(i.getTitle())) {
				initSel.add(i);
			}
		}
		st.setSelection(initSel);
		
//		st.setSize("100%", "100%");
	
	}
	


}
