import { Component, Output, EventEmitter, Input } from '@angular/core';
import { IBatch } from 'src/app/models/backend-types.model';
import { BackendService } from '../../backend.service'

@Component({
  selector: 'app-batch-picker',
  templateUrl: './batch-picker.component.html',
  styleUrls: ['./batch-picker.component.scss']
})
export class BatchPickerComponent {

  constructor(private backend: BackendService) { }

  private _datasetId: string | undefined;
  public get datasetId(): string | undefined {
    return this._datasetId;
  }
  @Input() public set datasetId(theDatasetId: string | undefined) {
    this._datasetId = theDatasetId;
    if (theDatasetId) {
      this.loadBatchesForDataset(theDatasetId);
    }
  }

  batches: IBatch[] | undefined;

  @Input() selectedBatch: string | undefined;
  @Output() selectedBatchChange = new EventEmitter<string>();

  loadBatchesForDataset(datasetId: string): void {
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

  selectBatch(batchId: string): void {
    this.selectedBatch = batchId;
    this.selectedBatchChange.emit(batchId);
  }
}