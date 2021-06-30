import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { Sample } from './models/backend-types.model'
import { IGeneSet, ISampleGroup } from './models/frontend-types.model'

class NamedItemStorage<T extends {name: string}> {

  private behaviorSubject: BehaviorSubject<Map<string, T>>;
  private _observable: Observable<Map<string, T>>;
  public get observable() {
    return this._observable;
  }

  constructor(private key: string) {
    const json = window.localStorage.getItem(key);
    const parsed = json ? JSON.parse(json) as unknown : [];
    const array = Array.isArray(parsed) ? parsed : [];
    this.behaviorSubject = this._observable = new BehaviorSubject(new Map<string, T>(array));
  }

  currentValue(): Map<string, T> {
    return this.behaviorSubject.value;
  }

  updateItems() {
    const json = JSON.stringify(Array.from(this.behaviorSubject.value));
    window.localStorage.setItem(this.key, json);
  }

  setItems(items: Map<string, T>) {
    this.behaviorSubject.next(items);
    this.updateItems();
  }

  setItem(key: string, item: T) {
    const itemMap = this.behaviorSubject.value;
    itemMap.set(key, item);
    this.behaviorSubject.next(itemMap)
    this.updateItems();
  }

  deleteItem(key: string) {
    const itemMap = this.behaviorSubject.value;
    itemMap.delete(key);
    this.behaviorSubject.next(itemMap);
    this.updateItems();
  }

  renameItem(oldName: string, newName: string): void {
    const item = this.currentValue().get(oldName);
    if (!item) throw new Error(`Tried to rename nonexistent item ${oldName}`);
    item.name = newName;
    this.setItem(newName, item);
    this.deleteItem(oldName);
    this.updateItems();
  }

  hasItem(key: string) {
    return this.behaviorSubject.value.has(key);
  }
}

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
