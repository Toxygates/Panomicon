package t.common.client.maintenance;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import t.common.client.rpc.BatchOperationsAsync;
import t.common.shared.Dataset;
import t.common.shared.maintenance.Batch;
import t.common.shared.maintenance.Instance;

import com.google.gwt.user.client.ui.VerticalPanel;

public class BatchEditor extends ManagedItemEditor {
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
  
  protected Set<String> instancesForBatch() {
    return new HashSet<String>(); //TODO
  }
  
  protected String datasetForBatch() {
    return ""; //TODO
  }
  
  @Override
  protected void triggerEdit() {  
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
      });
    } else {
      batchOps.update(b, editCallback());
    }
  }
}
