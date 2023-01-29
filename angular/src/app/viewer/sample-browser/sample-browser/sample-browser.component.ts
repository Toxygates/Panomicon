import { Component, OnInit } from '@angular/core';
import { BehaviorSubject, concat, Observable, of } from 'rxjs';
import { FetchedDataService } from '../../../shared/services/fetched-data.service';
import { Batch, Dataset } from '../../../shared/models/backend-types.model';
import { UserDataService } from '../../../shared/services/user-data.service';
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';
import { filter, map } from 'rxjs/operators';

@Component({
  selector: 'app-sample-browser',
  templateUrl: './sample-browser.component.html',
  styleUrls: ['./sample-browser.component.scss'],
})
export class SampleBrowserComponent implements OnInit {
  constructor(
    private userData: UserDataService,
    private fetchedData: FetchedDataService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  datasets$!: Observable<Dataset[] | null>;
  batches$!: Observable<Batch[] | null>;

  datasetId$!: BehaviorSubject<string | null>;
  batchId$!: BehaviorSubject<string | null>;

  modeOptions = [
    { name: 'Sample table', path: 'table' },
    { name: 'Batch statistics', path: 'batch-statistics' },
  ];

  nameForPath(path: string | undefined): string {
    return this.modeOptions.find((mode) => mode.path === path)?.name || '';
  }

  currentMode$ = concat(
    of(this.nameForPath(this.route.snapshot.firstChild?.url[0].path)),
    this.router.events.pipe(
      filter((event) => event instanceof NavigationEnd),
      map((event) =>
        this.nameForPath((event as NavigationEnd).url.split('/').pop())
      )
    )
  );

  ngOnInit(): void {
    this.datasetId$ = this.userData.selectedDataset$;
    this.batchId$ = this.userData.selectedBatch$;

    this.datasets$ = this.fetchedData.datasets$;
    this.batches$ = this.fetchedData.batches$;
  }
}
