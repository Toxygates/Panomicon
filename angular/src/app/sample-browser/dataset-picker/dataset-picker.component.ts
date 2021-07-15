import { Component, Output, EventEmitter, Input } from '@angular/core';
import { IDataset } from 'src/app/shared/models/backend-types.model';

@Component({
  selector: 'app-dataset-picker',
  templateUrl: './dataset-picker.component.html',
  styleUrls: ['./dataset-picker.component.scss']
})
export class DatasetPickerComponent {

  @Input() selectedDataset!: string | null;
  @Output() selectedDatasetChange = new EventEmitter<string>();

  @Input() datasets!: IDataset[] | null;

  selectDataset(datasetId: string): void {
    this.selectedDataset = datasetId;
    this.selectedDatasetChange.emit(datasetId);
  }
}
