import { Injectable } from '@angular/core';
import { BehaviorSubject, combineLatest, concat, EMPTY, Observable, of } from 'rxjs';
import { filter, map, pairwise, switchMap } from 'rxjs/operators';
import { Attribute, Batch, Dataset, Sample } from '../models/backend-types.model';
import { SampleFilter } from '../models/sample-filter.model';
import { BackendService } from './backend.service';
import { UserDataService } from './user-data.service';

@Injectable({
  providedIn: 'root'
})
export class FetchedDataService {

  roles$: Observable<string[]>;

  datasets$: BehaviorSubject<Dataset[] | null>;
  batches$: BehaviorSubject<Batch[] | null>;

  samples$: BehaviorSubject<Sample[] | null>;
  samplesMap$: BehaviorSubject<Map<string, Sample>>;
  sampleFilters$: BehaviorSubject<SampleFilter[]>
  filteredSamples$: BehaviorSubject<Sample[] | null>;
  attributes$: BehaviorSubject<Attribute[] | null>;
  attributeMap$: BehaviorSubject<Map<string, Attribute>>;
  requiredAttributes = new Set<string>();
  fetchedAttributes$: BehaviorSubject<Set<string>>;
  columnDefinitions$: BehaviorSubject<Tabulator.ColumnDefinition[]>;

  constructor(private backend: BackendService,
    private userData: UserDataService) {

    this.datasets$ = new BehaviorSubject<Dataset[] | null>(null);
    this.batches$ = new BehaviorSubject<Batch[] | null>(null);
    this.samples$ = new BehaviorSubject<Sample[] | null>(null);
    this.samplesMap$ = new BehaviorSubject<Map<string, Sample>>(new Map());
    this.sampleFilters$ = new BehaviorSubject<SampleFilter[]>([]);
    this.filteredSamples$ = new BehaviorSubject<Sample[] | null>(null);
    this.attributes$ = new BehaviorSubject<Attribute[] | null>(null);
    this.attributeMap$ = new BehaviorSubject<Map<string, Attribute>>(new Map());
    this.fetchedAttributes$ = new BehaviorSubject<Set<string>>(new Set());
    this.columnDefinitions$ = new BehaviorSubject<Tabulator.ColumnDefinition[]>([]);

    this.requiredAttributes.add("sample_id");

    this.roles$ = this.backend.getRoles();

    // fetch datasets
    this.backend.getDatasets().subscribe(datasets => this.datasets$.next(datasets));

    // fetch batches for selected dataset
    this.userData.selectedDataset$.pipe(
      filter(dataset => dataset != null),
      switchMap(datasetId => {
        return concat(of(null),
          this.backend.getBatchesForDataset(datasetId as string).pipe(
            map(result =>
              result.sort(function(a, b) {
                return a.id.localeCompare(b.id);
              }))));
      })
      ).subscribe(this.batches$);

    // fetch samples for selected batch
    this.userData.selectedBatch$.pipe(
      filter(batchId => batchId != null),
      switchMap(batchId => {
        return concat(of(null),
          this.backend.getSamplesForBatch(batchId as string));
      })
      ).subscribe(this.samples$);

    // create sample ID -> sample map
    this.samples$.pipe(
      map(samples => {
        const samplesMap = new Map<string, Sample>();
        samples?.forEach((s) => samplesMap.set(s.sample_id, s));
        return samplesMap;
      })).subscribe(this.samplesMap$);

    // generate filtered samples based on samples and sample filters
    combineLatest([this.samples$, this.sampleFilters$]).pipe(
      pairwise(),
      switchMap(([[samples1, filters1], [samples2, filters2]]) => {
        if (samples2 == null || filters2.length == 0) {
          return samples1 == null ? EMPTY : of(samples2);
        } else {
          if (samples1 === samples2 && filters1.length === 0 && filters2.length === 0) {
            return EMPTY;
          }
          const filteredSamples = samples2.filter(sample =>
            filters2.every(filter => filter.attribute && filter.passesFilter(sample[filter.attribute])));

          const includedTreatments = new Set<string>();
          filteredSamples.forEach(sample => {
            includedTreatments.add(sample.treatment);
            includedTreatments.add(sample.control_treatment);
          });
          const groupedFilteredSamples = samples2.filter(sample =>
            includedTreatments.has(sample.treatment)
          );
          return of(groupedFilteredSamples);
        }
      })
    ).subscribe(this.filteredSamples$);

    // clear sample filters when samples are fetched
    this.samples$.pipe(
      switchMap(_samples => {
        return of<SampleFilter[]>([]);
      })
    ).subscribe(this.sampleFilters$);

    // fetch attributes for batch
    this.userData.selectedBatch$.pipe(
      filter(batchId => batchId != null),
      switchMap(batchId => {
        return concat(
          this.backend.getAttributesForBatch(batchId as string));
      })
      ).subscribe(this.attributes$);

    // create attribute name -> attribute map
    this.attributes$.pipe(
      map(attributes => {
        const attributeMap = new Map<string, Attribute>();
        attributes?.forEach(a => attributeMap.set(a.id, a));
        return attributeMap;
      })).subscribe(this.attributeMap$);


    // initialize set of fetched attributes when samples and attributes are fetched
    combineLatest([this.samples$, this.attributeMap$]).pipe(
      map(([samples, attributeMap]) => {
        const fetchedAttributes = new Set<string>();
        if (samples && attributeMap) {
          samples.forEach(sample => {
            Object.keys(sample).forEach((attributeId) => {
              const found = attributeMap.get(attributeId);
              if (!found) throw new Error(`Sample had unknown attribute ${attributeId}`);
              fetchedAttributes.add(found.id);
            })
          })
        }
        return fetchedAttributes;
      })).subscribe(this.fetchedAttributes$);

    // reinitialize column definitions when samples are fetched
    this.samples$.pipe(
      switchMap(_samples => {
        return of(this.initialColumns());
      })
    ).subscribe(this.columnDefinitions$);
  }

  initialColumns(): Tabulator.ColumnDefinition[] {
    return [
      //{formatter:"rowSelection", titleFormatter:"rowSelection", align:"center", headerSort:false},
      {title: 'Sample ID', field: 'sample_id'},
    ];
  }

  fetchAttribute(attribute: Attribute): void {
    const samples = this.samples$.value;
    if (!samples) throw new Error("samples not defined");

    samples.forEach(sample => sample[attribute.id] = "Loading...");

    this.backend.getAttributeValues(samples.map(sample => sample.sample_id),
      [this.userData.selectedBatch$.value as string], [attribute.id]).subscribe(
      result => {
          this.fetchedAttributes$.value.add(attribute.id);
          result.forEach((element) => {
            const sample = this.samplesMap$.value.get(element.sample_id);
            if (sample) {
              sample[attribute.id] = element[attribute.id]
            }
          });
          samples.forEach(function(sample) {
            if (sample[attribute.id] == "Loading...") {
              sample[attribute.id] = "n/a";
            }
          })
          this.fetchedAttributes$.next(this.fetchedAttributes$.value);
      });
  }

}
