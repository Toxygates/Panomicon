import { Component, OnInit, Output, EventEmitter } from '@angular/core';
import { BackendService } from '../backend.service'

@Component({
  selector: 'app-batch-picker',
  templateUrl: './batch-picker.component.html',
  styleUrls: ['./batch-picker.component.scss']
})
export class BatchPickerComponent implements OnInit {

  constructor(private backend: BackendService) { }

  @Output() batchSelectedEvent = new EventEmitter<string>();

  batches: any;
  datasetId: string;
  selectedBatch: String;

  ngOnInit(): void {
  }

  loadBatchesForDataset(datasetId: string) {
    delete this.batches;
    delete this.selectedBatch;
    this.datasetId = datasetId;
    this.backend.getBatchesForDataset(datasetId)
      .subscribe(
        result => {
          this.batches = result;
        }
      )
  }

  selectBatch(batchId: string) {
    this.selectedBatch = batchId;
    this.batchSelectedEvent.emit(batchId);
  }
}
