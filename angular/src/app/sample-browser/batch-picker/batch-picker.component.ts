import { Component, Output, EventEmitter, Input } from '@angular/core';
import { Batch } from 'src/app/shared/models/backend-types.model';

@Component({
  selector: 'app-batch-picker',
  templateUrl: './batch-picker.component.html',
  styleUrls: ['./batch-picker.component.scss']
})
export class BatchPickerComponent {

  @Input() datasetId!: string | null;
  @Input() batches!: Batch[] | null;

  @Input() selectedBatch!: string | null;
  @Output() selectedBatchChange = new EventEmitter<string>();

  selectBatch(batchId: string): void {
    this.selectedBatch = batchId;
    this.selectedBatchChange.emit(batchId);
  }
}
