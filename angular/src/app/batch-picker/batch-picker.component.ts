import { Component, OnInit } from '@angular/core';
import { BackendService } from '../backend.service'

@Component({
  selector: 'app-batch-picker',
  templateUrl: './batch-picker.component.html',
  styleUrls: ['./batch-picker.component.scss']
})
export class BatchPickerComponent implements OnInit {

  constructor(private backend: BackendService) { }

  batches: any;

  ngOnInit(): void {
  }

  loadBatchesForDataset(datasetId: string) {
    delete this.batches;
    this.backend.getBatchesForDataset(datasetId)
      .subscribe(
        result => {
          this.batches = result;
        }
      )
  }

}
