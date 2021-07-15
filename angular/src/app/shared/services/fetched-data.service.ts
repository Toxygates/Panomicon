import { Injectable } from '@angular/core';
import { BehaviorSubject, concat, of } from 'rxjs';
import { filter, map, switchMap } from 'rxjs/operators';
import { IBatch, IDataset } from '../models/backend-types.model';
import { BackendService } from './backend.service';
import { UserDataService } from './user-data.service';

@Injectable({
  providedIn: 'root'
})
export class FetchedDataService {

  datasets$: BehaviorSubject<IDataset[] | undefined>;
  batches$: BehaviorSubject<IBatch[] | undefined>;

  constructor(private backend: BackendService,
    private userData: UserDataService) {

    this.datasets$ = new BehaviorSubject<IDataset[] | undefined>(undefined);
    this.backend.getDatasets().subscribe(this.datasets$);

    this.batches$ = new BehaviorSubject<IBatch[] | undefined>(undefined);
    this.userData.selectedDataset$.pipe(
      filter(dataset => dataset != null),
      switchMap(datasetId => {
        return concat(of(undefined),
          this.backend.getBatchesForDataset(datasetId as string).pipe(
            map(result =>
              result.sort(function(a, b) {
                return a.id.localeCompare(b.id);
              }))));
      })).subscribe(this.batches$);
  }

}
