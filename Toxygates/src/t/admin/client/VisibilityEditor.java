/*
 * Copyright (c) 2012-2019 Toxygates authors, National Institutes of Biomedical Innovation, Health and Nutrition (NIBIOHN), Japan.
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

import java.util.*;

import javax.annotation.Nullable;

import t.common.client.DataRecordSelector;
import t.common.shared.maintenance.Batch;
import t.common.shared.maintenance.Instance;

/**
 * GUI for editing the visibility of a batch.
 */
public class VisibilityEditor extends DataRecordSelector<Instance> {

  public VisibilityEditor(@Nullable Batch batch, Collection<Instance> instances) {
    super(instances);

    Set<Instance> initSel = new HashSet<Instance>();
    for (Instance i : instances) {
      if (batch != null && batch.getEnabledInstances().contains(i.getId())) {
        initSel.add(i);
      }
    }
    st.setSelection(initSel);

  }
}
