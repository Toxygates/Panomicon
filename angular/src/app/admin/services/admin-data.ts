import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, timer  } from 'rxjs';
import { mergeMap, scan, share, takeWhile, tap } from 'rxjs/operators'
import { FetchedDataService } from 'src/app/shared/services/fetched-data.service';
import { Batch, Instance, Dataset, Platform, ProgressUpdate } from './admin-types';
import { BackendService } from './backend.service';

@Injectable({
  providedIn: 'root'
})
export class AdminDataService {

  platforms$: BehaviorSubject<Platform[] | null>;
  batches$: BehaviorSubject<Batch[] | null>;
  datasets$: BehaviorSubject<Dataset[] | null>;
  instances$: BehaviorSubject<Instance[] | null>;

  progressState$: BehaviorSubject<ProgressUpdate | null>;
  progressMessages$: BehaviorSubject<string[]>;

  constructor(private backend: BackendService,
    private fetchedData: FetchedDataService,
  ) {
    this.platforms$ = new BehaviorSubject<Platform[] | null>(null);
    this.batches$ = new BehaviorSubject<Batch[] | null>(null);
    this.datasets$ = new BehaviorSubject<Dataset[] | null>(null);
    this.instances$ = new BehaviorSubject<Instance[] | null>(null);
    this.progressState$ = new BehaviorSubject<ProgressUpdate | null>(null);
    this.progressMessages$ = new BehaviorSubject<string[]>([]);

    // only attempt to fetch data if user is logged in as admin
    this.fetchedData.roles$.subscribe(roles => {
      if (roles?.includes('admin')) {
        this.backend.getBatches().subscribe(batches => this.batches$.next(batches));
        this.backend.getPlatforms().subscribe(platforms => this.platforms$.next(platforms));
        this.backend.getDatasets().subscribe(datasets => this.datasets$.next(datasets));
        this.backend.getInstances().subscribe(instances => this.instances$.next(instances));
        this.startTrackingProgress();
      }
    })
  }

  refreshDatasets(): void {
    this.datasets$.next(null);
    this.backend.getDatasets().subscribe(datasets => this.datasets$.next(datasets));
  }

  refreshBatches(): void {
    this.batches$.next(null);
    this.backend.getBatches().subscribe(batches => this.batches$.next(batches));
  }

  refreshPlatforms(): void {
    this.platforms$.next(null);
    this.backend.getPlatforms().subscribe(platforms => this.platforms$.next(platforms));
  }

  refreshInstances(): void {
    this.instances$.next(null);
    this.backend.getInstances().subscribe(instances => this.instances$.next(instances));
  }

  startTrackingProgress(): Observable<string[]> {
    const progress$ =
    timer(0, 2000).pipe(
      mergeMap(() => this.backend.getTaskProgress()),
      tap(progress => this.progressState$.next(progress)),
      takeWhile(progress => progress.finished === false, true),
      scan<ProgressUpdate, string[]>((acc, cur) => {
        return acc.concat(cur.messages);
      }, []),
      share()
    );

    progress$.subscribe(res => this.progressMessages$.next(res));

    return progress$;
  }
}
