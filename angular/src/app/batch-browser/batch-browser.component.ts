import { Component, ChangeDetectorRef } from '@angular/core';
import { BackendService } from '../backend.service';

enum BatchViewMode {
  SampleList = 'Sample list',
  SampleSearch = 'Sample search',
}

@Component({
  selector: 'app-batch-browser',
  templateUrl: './batch-browser.component.html',
  styleUrls: ['./batch-browser.component.scss']
})
export class BatchBrowserComponent {

  constructor(private backend: BackendService, 
    private changeDetector: ChangeDetectorRef) { }

  datasetId: string;
  batchId: string;
  samples: any;

  currentBatchViewMode = BatchViewMode.SampleList;
  batchViewModes = BatchViewMode;

  setViewMode(viewMode: BatchViewMode) {
    this.currentBatchViewMode = viewMode;
  }

  batchChanged(batchId: string) {
    console.log("batchId = " + batchId);
    this.batchId = batchId;
    delete this.samples;
    this.backend.getSamplesForBatch(batchId)
      .subscribe(
        result => {
          this.samples = result;
          this.changeDetector.detectChanges();
        }
      )
  }
}
