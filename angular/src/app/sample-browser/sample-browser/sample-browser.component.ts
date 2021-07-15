import { Component, OnInit } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { FetchedDataService } from 'src/app/shared/services/fetched-data.service';
import { IBatch, IDataset } from '../../shared/models/backend-types.model';
import { UserDataService } from '../../shared/services/user-data.service';

@Component({
  selector: 'app-sample-browser',
  templateUrl: './sample-browser.component.html',
  styleUrls: ['./sample-browser.component.scss']
})
export class SampleBrowserComponent implements OnInit {

  constructor(private userData: UserDataService,
    private fetchedData: FetchedDataService) {}

  datasets$!: Observable<IDataset[] | null>;
  batches$!: Observable<IBatch[] | null>;

  datasetId$!: BehaviorSubject<string | null>;
  batchId$!: BehaviorSubject<string | null>;

  ngOnInit(): void {
    this.datasetId$ = this.userData.selectedDataset$;
    this.batchId$ = this.userData.selectedBatch$;

    this.datasets$ = this.fetchedData.datasets$;
    this.batches$ = this.fetchedData.batches$;
  }
}
