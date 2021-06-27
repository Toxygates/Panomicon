import { Component, OnInit } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { IBatch, IDataset } from 'src/app/models/backend-types.model';
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

  datasets$!: Observable<IDataset[]>;
  batches$!: Observable<IBatch[]> | undefined;

  datasetId: string | undefined;
  batchId: string | undefined;

  ngOnInit(): void {
    this.datasetId = this.userData.getSelectedDataset();
    this.datasets$ = this.backend.getDatasets();
  }

  onSelectedDatasetChange(datasetId: string): void {
    this.datasetId = datasetId;
    this.userData.setSelectedDataset(datasetId);
    this.batches$ = this.backend.getBatchesForDataset(datasetId).pipe(
      map(result =>
        result.sort(function(a, b) {
          return a.id.localeCompare(b.id);
        })));
  }

  onSelectedBatchChange(batchId: string): void {
    this.batchId = batchId;
  }
}
