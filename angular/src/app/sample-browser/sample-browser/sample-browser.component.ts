import { Component, OnInit } from '@angular/core';
import { Sample } from 'src/app/models/backend-types.model';
import { BackendService } from '../../backend.service';
import { UserDataService } from '../../user-data.service';

@Component({
  selector: 'app-sample-browser',
  templateUrl: './sample-browser.component.html',
  styleUrls: ['./sample-browser.component.scss']
})
export class SampleBrowserComponent implements OnInit {

  constructor(private backend: BackendService, 
    private userData: UserDataService) {}

  datasetId: string | undefined;
  batchId: string | undefined;

  ngOnInit(): void {
    this.datasetId = this.userData.getSelectedDataset();
  }

  onSelectedDatasetChange(datasetId: string): void {
    this.datasetId = datasetId;
    this.userData.setSelectedDataset(datasetId);
  }

  onSelectedBatchChange(batchId: string): void {
    this.batchId = batchId;
  }
}