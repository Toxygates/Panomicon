/*
 * Copyright (c) 2012-2017 Toxygates authors, National Institutes of Biomedical Innovation, Health
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

import com.google.gwt.core.shared.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;

import t.common.client.maintenance.BatchEditor;
import t.common.client.maintenance.TaskCallback;
import t.common.shared.Dataset;
import t.common.shared.maintenance.Batch;
import t.common.shared.maintenance.Instance;

public class FullBatchEditor extends BatchEditor {

  protected VisibilityEditor visibility;
  protected ListBox datasetBox;

  protected final static MaintenanceServiceAsync maintenanceService = GWT.create(MaintenanceService.class);
  
  public FullBatchEditor(@Nullable Batch b, boolean addNew, Collection<Dataset> datasets,
      Collection<Instance> instances) {
    super(b, addNew, datasets, instances, maintenanceService);   
  }

  @Override
  protected void guiBeforeUploader(VerticalPanel vp, Batch b, boolean addNew) {
    vp.add(new Label("In dataset:"));
    datasetBox = new ListBox(); 
    
    if (b != null) {
      datasetBox.addItem(b.getDataset());
    }
    
    for (Dataset d : datasets) {
      if (b == null || !d.getTitle().equals(b.getDataset())) {
        datasetBox.addItem(d.getTitle());
      }
    }
    vp.add(datasetBox);

    vp.add(new Label("Visible in instances:"));
    visibility = new VisibilityEditor(b, instances);
    vp.add(visibility);
    visibility.setWidth("200px");
  }
  
  @Override
  protected void guiAfterUploader(VerticalPanel vp, Batch b, boolean addNew) {
    
  }
  
  @Override
  protected Set<String> instancesForBatch() {
    Set<String> instances = new HashSet<String>();
    for (Instance i : visibility.getSelection()) {
      instances.add(i.getTitle());
    }
    return instances;    
  }
  
  @Override
  protected String datasetForBatch() {
    return datasetBox.getSelectedValue();
  }
  
  @Override
  protected void triggerEdit() {  
    Batch b =
        new Batch(idText.getValue(), 0, commentArea.getValue(), new Date(), 
            instancesForBatch(), datasetForBatch());

    if (addNew) {
      if (uploader.canProceed()) {
        batchOps.addBatchAsync(b, new TaskCallback(
            this, "Upload batch", batchOps) {

          @Override
          protected void onCompletion() {
            onFinish();
            onFinishOrAbort();
          }
        });
      } else {
        Window.alert("Unable to proceed. Please make sure all required files have been uploaded.");
      }
    } else {
      batchOps.update(b, editCallback());
    }
  }

}
