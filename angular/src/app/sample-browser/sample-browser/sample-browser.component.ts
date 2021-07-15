import { Component, OnInit } from '@angular/core';
import { BehaviorSubject, concat, Observable, of } from 'rxjs';
import { filter, map, switchMap } from 'rxjs/operators';
import { IBatch, IDataset } from '../../shared/models/backend-types.model';
import { BackendService } from '../../shared/services/backend.service';
import { UserDataService } from '../../shared/services/user-data.service';

@Component({
  selector: 'app-sample-browser',
  templateUrl: './sample-browser.component.html',
  styleUrls: ['./sample-browser.component.scss']
})
export class SampleBrowserComponent implements OnInit {

  constructor(private backend: BackendService,
    private userData: UserDataService) {}

  datasets$!: Observable<IDataset[]>;
  batches$!: Observable<IBatch[] | undefined>;

  datasetId$!: BehaviorSubject<string | undefined>;
  batchId$!: BehaviorSubject<string | undefined>;

  ngOnInit(): void {
    this.datasetId$ = this.userData.selectedDataset$;
    this.batchId$ = this.userData.selectedBatch$;
    this.batches$ = this.datasetId$.pipe(
      filter(dataset => dataset != null),
      switchMap(datasetId => {
        return concat(of(undefined),
          this.backend.getBatchesForDataset(datasetId as string).pipe(
            map(result =>
              result.sort(function(a, b) {
                return a.id.localeCompare(b.id);
              }))));
      })
    );

    this.datasets$ = this.backend.getDatasets();
  }
}
