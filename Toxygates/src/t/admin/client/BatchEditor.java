/*
 * Copyright (c) 2012-2015 Toxygates authors, National Institutes of Biomedical Innovation, Health
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

package t.admin.client;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import t.admin.shared.Batch;
import t.admin.shared.Instance;
import t.common.shared.Dataset;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;

public class BatchEditor extends ManagedItemEditor {

  protected VisibilityEditor visibility;
  protected ListBox datasetBox;
  protected BatchUploader uploader;
  
  public BatchEditor(@Nullable Batch b, boolean addNew, Collection<Dataset> datasets,
      Collection<Instance> instances) {
    super(b, addNew);

    if (addNew) {
      uploader = new BatchUploader();
      vp.add(uploader);
    }

    vp.add(new Label("In dataset:"));
    datasetBox = new ListBox();
    datasetBox.addItem(b.getDataset());
    for (Dataset d : datasets) {
      if (!d.getTitle().equals(b.getDataset())) {
        datasetBox.addItem(d.getTitle());
      }
    }
    vp.add(datasetBox);

    vp.add(new Label("Visible in instances:"));
    visibility = new VisibilityEditor(b, instances);
    vp.add(visibility);
    visibility.setWidth("200px");

    addCommands();
  }

  @Override
  protected void triggerEdit() {
    Set<String> instances = new HashSet<String>();
    for (Instance i : visibility.getSelection()) {
      instances.add(i.getTitle());
    }

    Batch b =
        new Batch(idText.getValue(), 0, commentArea.getValue(), new Date(), instances,
            datasetBox.getSelectedValue());

    if (addNew && uploader.canProceed()) {
      maintenanceService.addBatchAsync(b, new TaskCallback(
          "Upload batch") {

        @Override
        void onCompletion() {          
          onFinish();
          onFinishOrAbort();
        }
      });
    } else {
      maintenanceService.update(b, editCallback());
    }

  }

}
