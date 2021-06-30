import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { Sample } from '../models/backend-types.model'
import { IGeneSet, ISampleGroup } from '../models/frontend-types.model'
import { NamedItemStorage } from './named-item-storage.class';

@Injectable({
  providedIn: 'root'
})
export class UserDataService {

  sampleGroups = new NamedItemStorage<ISampleGroup>(UserDataService.SAMPLE_GROUPS_KEY);
  geneSets = new NamedItemStorage<IGeneSet>(UserDataService.GENE_SETS_KEY);

  private enabledGroupsBehaviorSubject: BehaviorSubject<ISampleGroup[]>;
  enabledGroups$: Observable<ISampleGroup[]>

  static readonly SELECTED_DATASET_KEY: string ="selectedDataset_v1";
  static readonly SAMPLE_GROUPS_KEY: string = "sampleGroups_v2";
  static readonly GENE_SETS_KEY: string = "geneSets_v1";

  constructor() {
    this.enabledGroupsBehaviorSubject = this.enabledGroups$ = new BehaviorSubject([] as ISampleGroup[]);
    this.sampleGroups.observable.pipe(
      map(itemMap => Array.from(itemMap.values()).filter(group => group.enabled))
    ).subscribe(this.enabledGroupsBehaviorSubject);
  }

  public getSelectedDataset(): string | undefined {
    const dataset = window.localStorage.getItem(UserDataService.SELECTED_DATASET_KEY);
    return dataset ? dataset: undefined;
  }

  public setSelectedDataset(selectedDataset: string): void {
    window.localStorage.setItem(UserDataService.SELECTED_DATASET_KEY, selectedDataset);
  }

  canSelectGroup(group: ISampleGroup): boolean {
    const currentSelectedGroups = this.enabledGroupsBehaviorSubject.value;
    return (currentSelectedGroups.length == 0) ||
      ((currentSelectedGroups[0].organism == group.organism) &&
       (currentSelectedGroups[0].platform == group.platform))
  }

  saveSampleGroup(name: string, samples: Sample[]): void {
    const type = samples[0]["type"]
    const organism = samples[0]["organism"]
    const platform = samples[0]["platform_id"];

    const sampleIds = samples.map(s => s.sample_id);

    const newGroup = <ISampleGroup>{
      name: name,
      organism: organism,
      type: type,
      platform: platform,
      samples: sampleIds,
    };

    newGroup.enabled = this.canSelectGroup(newGroup);

    this.sampleGroups.setItem(name, newGroup);
    this.sampleGroups.updateItems();
  }
}
