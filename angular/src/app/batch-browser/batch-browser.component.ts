import { Component, OnInit, ViewChild, Output, EventEmitter } from '@angular/core';
import { BatchPickerComponent } from '../batch-picker/batch-picker.component';
import { BatchSamplesComponent } from '../batch-samples/batch-samples.component';

@Component({
  selector: 'app-batch-browser',
  templateUrl: './batch-browser.component.html',
  styleUrls: ['./batch-browser.component.scss']
})
export class BatchBrowserComponent implements OnInit {

  constructor() { }

  @ViewChild(BatchPickerComponent) batchPicker: BatchPickerComponent;
  @ViewChild(BatchSamplesComponent) batchSamples: BatchSamplesComponent;

  ngOnInit(): void {
  }

  showBatchesForDataset(datasetId: string) {
    this.batchPicker.loadBatchesForDataset(datasetId);
  }

  selectBatch(batchId: string) {
    this.batchSamples.loadSamplesForBatch(batchId);
  }
}
