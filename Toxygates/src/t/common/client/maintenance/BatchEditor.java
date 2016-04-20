package t.common.client.maintenance;

import java.util.Collection;
import java.util.Date;
import java.util.Set;

import t.common.client.rpc.BatchOperationsAsync;
import t.common.shared.Dataset;
import t.common.shared.maintenance.Batch;
import t.common.shared.maintenance.Instance;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.VerticalPanel;

abstract public class BatchEditor extends ManagedItemEditor {
  protected BatchUploader uploader;
  
  final protected Collection<Dataset> datasets;
  final protected Collection<Instance> instances;
  final protected BatchOperationsAsync batchOps;
  
  public BatchEditor(Batch b, boolean addNew, Collection<Dataset> datasets,
      Collection<Instance> instances, BatchOperationsAsync batchOps) {
    super(b, addNew);
    this.datasets = datasets;
    this.instances = instances;
    this.batchOps = batchOps;
    guiBeforeUploader(vp, b, addNew);

    if (addNew) { 
      uploader = new BatchUploader();
      vp.add(uploader);
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
  
  @Override
  protected void triggerEdit() {
    if (idText.getValue().equals("")) {
      Window.alert("ID cannot be empty");
      return;
    }
    
    Batch b =
        new Batch(idText.getValue(), 0, commentArea.getValue(), new Date(), 
            instancesForBatch(), datasetForBatch());

    if (addNew && uploader.canProceed()) {
      batchOps.addBatchAsync(b, new TaskCallback(
          "Upload batch", batchOps) {

        @Override
        protected void onCompletion() {          
          onFinish();
          onFinishOrAbort();
        }
        
        @Override
        protected void onFailure() {
          onFinishOrAbort();
        }
      });
    } else {
      batchOps.update(b, editCallback());
    }
  }
  
  protected void onFinishOrAbort() {
    uploader.resetAll();
  }
  
  protected void onAbort() {
    uploader.resetAll();
  }
}
