import { Injectable } from '@angular/core';
import { BehaviorSubject, combineLatest, concat, of } from 'rxjs';
import { filter, map, switchMap } from 'rxjs/operators';
import { IAttribute, IBatch, IDataset, Sample } from '../models/backend-types.model';
import { BackendService } from './backend.service';
import { UserDataService } from './user-data.service';

@Injectable({
  providedIn: 'root'
})
export class FetchedDataService {

  datasets$: BehaviorSubject<IDataset[] | null>;
  batches$: BehaviorSubject<IBatch[] | null>;

  samples$: BehaviorSubject<Sample[] | null>;
  samplesMap$: BehaviorSubject<Map<string, Sample>>;
  attributes$: BehaviorSubject<IAttribute[] | null>;
  attributeMap$: BehaviorSubject<Map<string, IAttribute>>;
  requiredAttributes = new Set<string>();
  fetchedAttributes$: BehaviorSubject<Set<string>>;

  constructor(private backend: BackendService,
    private userData: UserDataService) {

    this.datasets$ = new BehaviorSubject<IDataset[] | null>(null);
    this.backend.getDatasets().subscribe(datasets => this.datasets$.next(datasets));

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
      })
      ).subscribe(this.batches$);

    this.samples$ = new BehaviorSubject<Sample[] | null>(null);
    this.userData.selectedBatch$.pipe(
      filter(batchId => batchId != null),
      switchMap(batchId => {
        return concat(of(null),
          this.backend.getSamplesForBatch(batchId as string));
      })
      ).subscribe(this.samples$);

    this.samplesMap$ = new BehaviorSubject<Map<string, Sample>>(new Map());
    this.samples$.pipe(
      map(samples => {
        const samplesMap = new Map<string, Sample>();
        samples?.forEach((s) => samplesMap.set(s.sample_id, s));
        return samplesMap;
      })).subscribe(this.samplesMap$);

    this.attributes$ = new BehaviorSubject<IAttribute[] | null>(null);
    this.userData.selectedBatch$.pipe(
      filter(batchId => batchId != null),
      switchMap(batchId => {
        return concat(
          this.backend.getAttributesForBatch(batchId as string));
      })
      ).subscribe(this.attributes$);

    this.attributeMap$ = new BehaviorSubject<Map<string, IAttribute>>(new Map());
    this.attributes$.pipe(
      map(attributes => {
        const attributeMap = new Map<string, IAttribute>();
        attributes?.forEach(a => attributeMap.set(a.id, a));
        return attributeMap;
      })).subscribe(this.attributeMap$);

    this.requiredAttributes.add("sample_id");

    this.fetchedAttributes$ = new BehaviorSubject<Set<string>>(new Set());
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
  }

  fetchAttribute(attribute: IAttribute): void {
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
