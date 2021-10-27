import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, timer  } from 'rxjs';
import { mergeMap, scan, share, takeWhile, tap } from 'rxjs/operators'
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

  constructor(private backend: BackendService) {
    this.platforms$ = new BehaviorSubject<Platform[] | null>(null);
    this.backend.getPlatforms().subscribe(platforms => this.platforms$.next(platforms));

    this.batches$ = new BehaviorSubject<Batch[] | null>(null);
    this.backend.getBatches().subscribe(batches => this.batches$.next(batches));

    this.datasets$ = new BehaviorSubject<Dataset[] | null>(null);
    this.backend.getDatasets().subscribe(datasets => this.datasets$.next(datasets));

    this.instances$ = new BehaviorSubject<Instance[] | null>(null);
    this.backend.getInstances().subscribe(instances => this.instances$.next(instances));

    this.progressState$ = new BehaviorSubject<ProgressUpdate | null>(null);
    this.progressMessages$ = new BehaviorSubject<string[]>([]);
    this.startTrackingProgress();
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
