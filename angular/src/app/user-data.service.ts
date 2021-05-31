import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { ISampleGroup } from './models/sample-group.model'

@Injectable({
  providedIn: 'root'
})
export class UserDataService {

  sampleGroupsBehaviorSubject: BehaviorSubject<Map<string, ISampleGroup>>;
  enabledGroupsBehaviorSubject: BehaviorSubject<ISampleGroup[]>;
  private sampleGroups: Map<string, ISampleGroup>;

  static readonly SAMPLE_GROUPS_KEY: string = "sampleGroups_v2";

  constructor() {
    let sampleGroupsJson = window.localStorage.getItem(UserDataService.SAMPLE_GROUPS_KEY);
    if (sampleGroupsJson) {
      this.sampleGroups = new Map(JSON.parse(sampleGroupsJson));
    } else {
      this.sampleGroups = new Map();
    }
    this.sampleGroupsBehaviorSubject = new BehaviorSubject(this.sampleGroups);
    this.enabledGroupsBehaviorSubject = new BehaviorSubject(this.getEnabledSampleGroups());
  }

  private updateSampleGroups() {
    let sampleGroupsJson = JSON.stringify(Array.from(this.sampleGroups.entries()));
    window.localStorage.setItem(UserDataService.SAMPLE_GROUPS_KEY, sampleGroupsJson);
    this.sampleGroupsBehaviorSubject.next(this.sampleGroups);
    this.enabledGroupsBehaviorSubject.next(this.getEnabledSampleGroups());
  }

  saveSampleGroups(sampleGroups: Map<string, ISampleGroup>) {
    this.sampleGroups = sampleGroups;
    this.updateSampleGroups();
  }

  saveSampleGroup(name: string, samples: string[]) {
    this.sampleGroups.set(name, <ISampleGroup>{
      name: name,
      samples: samples,
      enabled: true,
    });
    this.updateSampleGroups();
  }

  renameSampleGroup(oldName: string, newName: string) {
    let group = this.sampleGroups.get(oldName);
    group.name = newName;
    this.sampleGroups.set(newName, group);
    this.sampleGroups.delete(oldName);
    this.updateSampleGroups();
  }

  deleteSampleGroup(name: string) {
    this.sampleGroups.delete(name);
    this.updateSampleGroups();
  }

  isAcceptableGroupName(name: string) {
    return !this.sampleGroups.has(name);
  }

  private getEnabledSampleGroups() {
    let enabledGroups = [];
    for (let groupName of Array.from(this.sampleGroups.keys()).sort()) {
      if (this.sampleGroups.get(groupName).enabled) {
        enabledGroups.push(this.sampleGroups.get(groupName));
      }
    }
    return enabledGroups;
  }
}
