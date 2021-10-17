import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { Batch, Instance, Dataset, Platform } from './admin-types';
import { BackendService } from './backend.service';

@Injectable({
  providedIn: 'root'
})
export class AdminDataService {

  platforms$: BehaviorSubject<Platform[] | null>;
  batches$: BehaviorSubject<Batch[] | null>;
  datasets$: BehaviorSubject<Dataset[] | null>;
  instances$: BehaviorSubject<Instance[] | null>;

  constructor(private backend: BackendService) {
    this.platforms$ = new BehaviorSubject<Platform[] | null>(null);
    this.backend.getPlatforms().subscribe(platforms => this.platforms$.next(platforms));

    this.batches$ = new BehaviorSubject<Batch[] | null>(null);
    this.backend.getBatches().subscribe(batches => this.batches$.next(batches));

    this.datasets$ = new BehaviorSubject<Dataset[] | null>(null);
    this.backend.getDatasets().subscribe(datasets => this.datasets$.next(datasets));

    this.instances$ = new BehaviorSubject<Instance[] | null>(null);
    this.backend.getInstances().subscribe(instances => this.instances$.next(instances));
  }

  refreshDatasets(): void {
    this.datasets$.next(null);
    this.backend.getDatasets().subscribe(datasets => this.datasets$.next(datasets));
  }

}
