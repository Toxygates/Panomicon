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

package t.common.client.maintenance;

import java.util.*;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.VerticalPanel;

import t.common.client.rpc.BatchOperationsAsync;
import t.common.client.rpc.MaintenanceOperationsAsync;
import t.shared.common.Dataset;
import t.shared.common.maintenance.*;

abstract public class BatchEditor extends ManagedItemEditor {
  @Nullable protected BatchUploader uploader;
  
  final protected Collection<Dataset> datasets;
  final protected Collection<Instance> instances;
  final protected BatchOperationsAsync batchOps;
  
  protected CheckBox recalculate;

  public BatchEditor(Batch b, boolean addNew, Collection<Dataset> datasets,
      Collection<Instance> instances, BatchOperationsAsync batchOps) {
    super(b, addNew);
    this.datasets = datasets;
    this.instances = instances;
    this.batchOps = batchOps;

    guiBeforeUploader(vp, b, addNew);
    uploader = new BatchUploader(addNew);
    vp.add(uploader);
    if (!addNew) {
      recalculate = new CheckBox("Recalculate folds and series");
      recalculate.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          boolean checked = ((CheckBox) event.getSource()).getValue();
          if (checked && !uploader.canProceed()) {
            Window.alert("Cannot recalculate folds and series without new metadata");
            recalculate.setValue(false);
          }
        }
      });
      vp.add(recalculate);
    }
    guiAfterUploader(vp, b, addNew);
    
    addCommands();
  }

  protected void guiBeforeUploader(VerticalPanel vp, Batch b, boolean addNew) {    
  }
  
  protected void guiAfterUploader(VerticalPanel vp, Batch b, boolean addNew) {    
  }
  
  abstract protected Set<String> instancesForBatch();
  
  abstract protected String datasetForBatch();
  
  protected void onBatchUploadBegan() {}

  @Override
  protected void triggerEdit() {
    if (idText.getValue().equals("")) {
      Window.alert("ID cannot be empty");
      return;
    }
    okButton.setEnabled(false);
    
    Batch b = new Batch(idText.getValue(), 0, commentArea.getValue(), new Date(),
            instancesForBatch(), datasetForBatch());

    if (addNew) {
      if (uploader.canProceed()) {
        batchOps.addBatchAsync(b, callback("Upload batch"));
      } else {
        Window.alert(
            "Unable to proceed. Please make sure all required files have been uploaded.");
        okButton.setEnabled(true);

      }
    } else {
      if (uploader.canProceed()) {
        batchOps.updateBatchMetadataAsync(b, recalculate.getValue(),
            callback("Update batch metadata"));
      } else {
        batchOps.update(b, editCallback());
      }
    }
  }
  
  protected TaskCallback callback(String title) {
    return new BETaskCallback(logger, title, batchOps);
  }

  protected class BETaskCallback extends TaskCallback {
    public BETaskCallback(Logger l, String title, MaintenanceOperationsAsync maintenanceOps) {
      super(l, title, maintenanceOps);
    }

    @Override
    public void onSuccess(Void result) {
      super.onSuccess(result);
      onBatchUploadBegan();
    }

    @Override
    protected void onCompletion() {
      onFinish();
      onFinishOrAbort();
    }

    @Override
    protected void onCancelled() {
      onError();
    }

    @Override
    protected void handleFailure(Throwable caught) {
      if (caught instanceof BatchUploadException) {
        BatchUploadException exception = (BatchUploadException) caught;
        if (exception.idWasBad) {
          idText.setText("");
        }
        if (exception.metadataWasBad) {
          uploader.metadata.setFailure();
        }
        if (exception.normalizedDataWasBad) {
          uploader.data.setFailure();
        }
        okButton.setEnabled(true);
      } else {
        onError();
      }
    }
  }

  @Override
  protected void onError() {
    okButton.setEnabled(true);
  }
}
