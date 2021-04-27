import { Component, Output, EventEmitter, Input } from '@angular/core';
import { BackendService } from '../backend.service'

@Component({
  selector: 'app-batch-picker',
  templateUrl: './batch-picker.component.html',
  styleUrls: ['./batch-picker.component.scss']
})
export class BatchPickerComponent {

  constructor(private backend: BackendService) { }

  private _datasetId: string;
  public get datasetId() {
    return this._datasetId;
  }
  @Input() public set datasetId(theDatasetId: string) {
    this._datasetId = theDatasetId;
    if (theDatasetId) {
      this.loadBatchesForDataset(theDatasetId);
    }
  }

  batches: any;

  @Input() selectedBatch: string;
  @Output() selectedBatchChange = new EventEmitter<string>();

  loadBatchesForDataset(datasetId: string) {
    delete this.batches;
    this.backend.getBatchesForDataset(datasetId)
      .subscribe(
        result => {
          result.sort(function(a, b) {
            return a.id.localeCompare(b.id);
          });
          this.batches = result;
        }
      )
  }

  selectBatch(batchId: string) {
    this.selectedBatch = batchId;
    this.selectedBatchChange.emit(batchId);
  }
}
