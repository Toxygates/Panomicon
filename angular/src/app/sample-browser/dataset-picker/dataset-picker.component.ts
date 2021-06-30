import { Component, Output, EventEmitter, Input } from '@angular/core';
import { IDataset } from 'src/app/shared/models/backend-types.model';
import { BackendService } from '../../shared/services/backend.service'

@Component({
  selector: 'app-dataset-picker',
  templateUrl: './dataset-picker.component.html',
  styleUrls: ['./dataset-picker.component.scss']
})
export class DatasetPickerComponent {

  constructor(private backend: BackendService) { }

  @Input() selectedDataset: string | undefined;
  @Output() selectedDatasetChange = new EventEmitter<string>();

  @Input() datasets!: IDataset[] | null;

  selectDataset(datasetId: string): void {
    this.selectedDataset = datasetId;
    this.selectedDatasetChange.emit(datasetId);
  }
}
