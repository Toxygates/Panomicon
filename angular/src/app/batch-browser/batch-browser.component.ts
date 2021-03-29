import { Component } from '@angular/core';

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

  datasetId: string;
  batchId: string;

  currentBatchViewMode = BatchViewMode.SampleList;
  batchViewModes = BatchViewMode;

  setViewMode(viewMode: BatchViewMode) {
    this.currentBatchViewMode = viewMode;
  }
}
