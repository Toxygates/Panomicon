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

  datasets$: BehaviorSubject<IDataset[] | null>;
  batches$: BehaviorSubject<IBatch[] | null>;

  constructor(private backend: BackendService,
    private userData: UserDataService) {

    this.datasets$ = new BehaviorSubject<IDataset[] | null>(null);
    this.backend.getDatasets().subscribe(this.datasets$);

    this.batches$ = new BehaviorSubject<IBatch[] | null>(null);
    this.userData.selectedDataset$.pipe(
      filter(dataset => dataset != null),
      switchMap(datasetId => {
        return concat(of(null),
          this.backend.getBatchesForDataset(datasetId as string).pipe(
            map(result =>
              result.sort(function(a, b) {
                return a.id.localeCompare(b.id);
              }))));
      })).subscribe(this.batches$);
  }

}
