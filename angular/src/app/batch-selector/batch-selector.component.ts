import { Component, OnInit, ViewChild, Output, EventEmitter } from '@angular/core';
import { BatchPickerComponent } from '../batch-picker/batch-picker.component';

@Component({
  selector: 'app-batch-selector',
  templateUrl: './batch-selector.component.html',
  styleUrls: ['./batch-selector.component.scss']
})
export class BatchSelectorComponent implements OnInit {

  constructor() { }

  @Output() batchSelectedEvent = new EventEmitter<string>();

  datasetChosen: boolean = false;
  @ViewChild(BatchPickerComponent) child: BatchPickerComponent;

  ngOnInit(): void {
  }

  showBatchesForDataset(datasetId: string) {
    this.datasetChosen = true;
    this.child.loadBatchesForDataset(datasetId);
  }

  selectBatch(batchId: string) {
    this.batchSelectedEvent.emit(batchId);
  }
}
