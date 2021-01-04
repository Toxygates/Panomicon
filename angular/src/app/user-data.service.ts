import { Injectable } from '@angular/core';
import { ISampleGroup } from './models/sample-group.model'

@Injectable({
  providedIn: 'root'
})
export class UserDataService {

  sampleGroups: Map<string, ISampleGroup>;

  static readonly SAMPLE_GROUPS_KEY: string = "sampleGroups_v2";

  constructor() {
    let sampleGroupsJson = window.localStorage.getItem(UserDataService.SAMPLE_GROUPS_KEY);
    if (sampleGroupsJson) {
      this.sampleGroups = new Map(JSON.parse(sampleGroupsJson));
    } else {
      this.sampleGroups = new Map();
    }
  }

  serializeSampleGroups() {
    let sampleGroupsJson = JSON.stringify(Array.from(this.sampleGroups.entries()));
    window.localStorage.setItem(UserDataService.SAMPLE_GROUPS_KEY, sampleGroupsJson);
  }

  saveSampleGroups(sampleGroups: Map<string, ISampleGroup>) {
    this.sampleGroups = sampleGroups;
    this.serializeSampleGroups();
  }

  saveSampleGroup(name: string, samples: string[]) {
    this.sampleGroups.set(name, <ISampleGroup>{
      name: name,
      samples: samples,
      enabled: true,
    });
    this.serializeSampleGroups();
  }

  deleteSampleGroup(name: string) {
    this.sampleGroups.delete(name);
    this.serializeSampleGroups();
  }

  getSampleGroups() {
    return this.sampleGroups;
  }
}
