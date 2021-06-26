import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { Sample } from './models/backend-types.model'
import { ISampleGroup } from './models/frontend-types.model'

@Injectable({
  providedIn: 'root'
})
export class UserDataService {

  sampleGroupsBehaviorSubject: BehaviorSubject<Map<string, ISampleGroup>>;
  enabledGroupsBehaviorSubject: BehaviorSubject<ISampleGroup[]>;
  private sampleGroups: Map<string, ISampleGroup>;

  static readonly SELECTED_DATASET_KEY: string ="selectedDataset_v1";
  static readonly SAMPLE_GROUPS_KEY: string = "sampleGroups_v2";

  constructor() {
    const sampleGroupsJson = window.localStorage.getItem(UserDataService.SAMPLE_GROUPS_KEY);
    if (sampleGroupsJson) {
      this.sampleGroups = new Map(JSON.parse(sampleGroupsJson));
    } else {
      this.sampleGroups = new Map<string, ISampleGroup>();
    }
    this.sampleGroupsBehaviorSubject = new BehaviorSubject(this.sampleGroups);
    this.enabledGroupsBehaviorSubject = new BehaviorSubject(this.enabledSampleGroups());
  }

  public getSelectedDataset(): string | undefined {
    const dataset = window.localStorage.getItem(UserDataService.SELECTED_DATASET_KEY);
    return dataset ? dataset: undefined;
  }

  public setSelectedDataset(selectedDataset: string): void {
    window.localStorage.setItem(UserDataService.SELECTED_DATASET_KEY, selectedDataset);
  }

  private updateSampleGroups() {
    const sampleGroupsJson = JSON.stringify(Array.from(this.sampleGroups.entries()));
    window.localStorage.setItem(UserDataService.SAMPLE_GROUPS_KEY, sampleGroupsJson);
    this.sampleGroupsBehaviorSubject.next(this.sampleGroups);
    this.enabledGroupsBehaviorSubject.next(this.enabledSampleGroups());
  }

  canSelectGroup(group: ISampleGroup): boolean {
    const currentSelectedGroups = this.enabledGroupsBehaviorSubject.value;
    return (currentSelectedGroups.length == 0) ||
      ((currentSelectedGroups[0].organism == group.organism) &&
       (currentSelectedGroups[0].platform == group.platform))
  }

  saveSampleGroups(sampleGroups: Map<string, ISampleGroup>): void {
    this.sampleGroups = sampleGroups;
    this.updateSampleGroups();
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

    this.sampleGroups.set(name, newGroup);
    this.updateSampleGroups();
  }

  renameSampleGroup(oldName: string, newName: string): void {
    const group = this.sampleGroups.get(oldName);
    if (!group) throw new Error(`Tried to rename nonexistent group ${oldName}`);
    group.name = newName;
    this.sampleGroups.set(newName, group);
    this.sampleGroups.delete(oldName);
    this.updateSampleGroups();
  }

  deleteSampleGroup(name: string): void {
    this.sampleGroups.delete(name);
    this.updateSampleGroups();
  }

  isAcceptableGroupName(name: string): boolean {
    return !this.sampleGroups.has(name);
  }

  private enabledSampleGroups(): ISampleGroup[] {
    return Array.from(this.sampleGroups.values()).filter(group => group.enabled);
  }
}
