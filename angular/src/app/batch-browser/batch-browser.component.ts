import { Component, OnInit } from '@angular/core';
import { BackendService } from '../backend.service';
import { UserDataService } from '../user-data.service';

@Component({
  selector: 'app-batch-browser',
  templateUrl: './batch-browser.component.html',
  styleUrls: ['./batch-browser.component.scss']
})
export class BatchBrowserComponent implements OnInit {

  constructor(private backend: BackendService, 
    private userData: UserDataService) {}

  datasetId: string;
  batchId: string;
  samples;

  ngOnInit(): void {
    this.datasetId = this.userData.getSelectedDataset();
  }

  onSelectedDatasetChange(datasetId: string): void {
    this.datasetId = datasetId;
    this.userData.setSelectedDataset(datasetId);
  }

  batchChanged(batchId: string): void {
    this.batchId = batchId;
    delete this.samples;
    this.backend.getSamplesForBatch(batchId)
      .subscribe(
        result => {
          this.samples = result;
        }
      )
  }
}
