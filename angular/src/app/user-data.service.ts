import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class UserDataService {

  sampleGroups: Map<string, string[]>;

  static readonly SAMPLE_GROUPS_KEY: string = "sampleGroups_v1";

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

  saveSampleGroup(name: string, samples: string[]) {
    this.sampleGroups.set(name, samples);
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
