import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { Sample } from '../models/backend-types.model'
import { IGeneSet, ISampleGroup } from '../models/frontend-types.model'
import { SampleGroupLogic } from '../models/sample-group-logic.class';
import { NamedItemStorage } from './named-item-storage.class';

@Injectable({
  providedIn: 'root'
})
export class UserDataService {

  sampleGroups!: NamedItemStorage<ISampleGroup>;
  geneSets!: NamedItemStorage<IGeneSet>;

  private enabledGroupsBehaviorSubject: BehaviorSubject<ISampleGroup[]>;
  enabledGroups$: Observable<ISampleGroup[]>
  platform$: Observable<string | undefined>;

  static readonly SELECTED_DATASET_KEY: string ="selectedDataset_v1";
  static readonly SAMPLE_GROUPS_KEY: string = "sampleGroups_v2";
  static readonly GENE_SETS_KEY: string = "geneSets_v1";

  constructor() {
    this.sampleGroups = new NamedItemStorage(this.deserializeArray(UserDataService.SAMPLE_GROUPS_KEY));
    this.sampleGroups.observable.subscribe(newValue => {
      const json = JSON.stringify(Array.from(newValue));
      window.localStorage.setItem(UserDataService.SAMPLE_GROUPS_KEY, json);
    });

    this.geneSets = new NamedItemStorage(this.deserializeArray(UserDataService.GENE_SETS_KEY));
    this.geneSets.observable.subscribe(newValue => {
      const json = JSON.stringify(Array.from(newValue));
      window.localStorage.setItem(UserDataService.GENE_SETS_KEY, json);
    });

    this.enabledGroupsBehaviorSubject = this.enabledGroups$ = new BehaviorSubject([] as ISampleGroup[]);
    this.sampleGroups.observable.pipe(
      map(itemMap => Array.from(itemMap.values()).filter(group => group.enabled))
    ).subscribe(this.enabledGroupsBehaviorSubject);
    this.platform$ = this.enabledGroups$.pipe(
      map(groups => groups.length > 0 ? groups[0].platform : undefined)
    );
  }

  // TODO move this elsewhere
  private deserializeArray<T>(key: string): Map<string, T> {
    const json = window.localStorage.getItem(key);
    const parsed = json ? JSON.parse(json) as unknown : [];
    const array = Array.isArray(parsed) ? parsed : [];
    return new Map<string, T>(array)
  }

  public getSelectedDataset(): string | undefined {
    const dataset = window.localStorage.getItem(UserDataService.SELECTED_DATASET_KEY);
    return dataset ? dataset: undefined;
  }

  public setSelectedDataset(selectedDataset: string): void {
    window.localStorage.setItem(UserDataService.SELECTED_DATASET_KEY, selectedDataset);
  }

  canSelectGroup(group: ISampleGroup): boolean {
    return SampleGroupLogic.canSelectGroup(group, this.enabledGroupsBehaviorSubject.value);
  }

  saveSampleGroup(name: string, samples: Sample[]): void {
    const newGroup = SampleGroupLogic.createSampleGroup(name, samples,
      this.enabledGroupsBehaviorSubject.value);
    this.sampleGroups.saveItem(newGroup);
  }
}
