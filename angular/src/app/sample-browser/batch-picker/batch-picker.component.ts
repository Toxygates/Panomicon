import { Component, Output, EventEmitter, Input } from '@angular/core';
import { IBatch } from 'src/app/shared/models/backend-types.model';
import { BackendService } from '../../shared/services/backend.service'

@Component({
  selector: 'app-batch-picker',
  templateUrl: './batch-picker.component.html',
  styleUrls: ['./batch-picker.component.scss']
})
export class BatchPickerComponent {

  constructor(private backend: BackendService) { }

  @Input() datasetId: string | undefined | null;
  @Input() batches: IBatch[] | null | undefined;

  @Input() selectedBatch: string | undefined | null;
  @Output() selectedBatchChange = new EventEmitter<string>();

  selectBatch(batchId: string): void {
    this.selectedBatch = batchId;
    this.selectedBatchChange.emit(batchId);
  }
}
