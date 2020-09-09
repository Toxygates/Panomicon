import { Component, OnInit, Output, EventEmitter } from '@angular/core';
import { BackendService } from '../backend.service'

@Component({
  selector: 'app-dataset-picker',
  templateUrl: './dataset-picker.component.html',
  styleUrls: ['./dataset-picker.component.scss']
})
export class DatasetPickerComponent implements OnInit {
  
  constructor(private backend: BackendService) { }

  @Output() datasetSelectedEvent = new EventEmitter<string>();

  datasets: any;

  ngOnInit(): void {
    this.backend.getDatasets()
      .subscribe(
        result => {
          this.datasets = result;
        })
  }

  selectDataset(datasetId: string) {
    this.datasetSelectedEvent.emit(datasetId);
  }

}
