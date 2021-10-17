import { Component, Output, EventEmitter, Input } from '@angular/core';
import { Dataset } from '../../../shared/models/backend-types.model';

@Component({
  selector: 'app-dataset-picker',
  templateUrl: './dataset-picker.component.html',
  styleUrls: ['./dataset-picker.component.scss']
})
export class DatasetPickerComponent {

  @Input() selectedDataset!: string | null;
  @Output() selectedDatasetChange = new EventEmitter<string>();

  @Input() datasets!: Dataset[] | null;

  selectDataset(datasetId: string): void {
    this.selectedDataset = datasetId;
    this.selectedDatasetChange.emit(datasetId);
  }
}
