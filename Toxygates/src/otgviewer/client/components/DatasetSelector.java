/*
 * Copyright (c) 2012-2018 Toxygates authors, National Institutes of Biomedical Innovation, Health
 * and Nutrition (NIBIOHN), Japan.
 * 
 * This file is part of Toxygates.
 * 
 * Toxygates is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * Toxygates is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Toxygates. If not,
 * see <http://www.gnu.org/licenses/>.
 */

package otgviewer.client.components;

import java.util.*;

import com.google.gwt.user.client.ui.*;

import t.common.client.*;
import t.common.shared.Dataset;

public class DatasetSelector extends Composite {
  final static private String message = "Please select the datasets you want to work with.";
  final protected DataRecordSelector<Dataset> selector;

  public DatasetSelector(Collection<Dataset> items, Collection<Dataset> selectedItems) {
    final String USERDATA = "Shared user data";
    
    Collection<Dataset> gi = Dataset.groupUserShared(USERDATA, items);    
    selector = new DataRecordSelector<>(gi);
    
    /**
     * selectedItems can come from user storage, but dynamically defined
     * datasets (currently, only shared user data) can change over time
     * and the latest version comes from the server side in "items".
     * For this reason, we need to translate the selected ids into the
     * corresponding latest versions of the datasets.
     */
    Set<String> selectedIds = new HashSet<String>();
    for (Dataset d: Dataset.groupUserShared(USERDATA, selectedItems)) {
      selectedIds.add(d.getTitle());
    }
    List<Dataset> selectedGrouped = new ArrayList<Dataset>();
    for (Dataset d: gi) {
      if (selectedIds.contains(d.getTitle())) {
        selectedGrouped.add(d);
      }
    }
    
    selector.setSelection(selectedGrouped);

    VerticalPanel vp = new VerticalPanel();
    vp.add(new Label(message));
    vp.add(selector);
    initWidget(vp);

    List<RunCommand> commands = new ArrayList<RunCommand>();
    commands.add(new RunCommand("OK", () -> onOK()));
    commands.add(new RunCommand("Cancel", () -> onCancel()));      
    vp.add(Utils.makeButtons(commands));
  }

  protected Collection<Dataset> getSelected() {
    return Dataset.ungroup(selector.getSelection());
  }
  
  public void onOK() {}

  public void onCancel() {}
}
