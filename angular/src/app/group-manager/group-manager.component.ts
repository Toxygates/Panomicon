import { Component, OnDestroy, OnInit } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { UserDataService } from '../user-data.service';
import { ISampleGroup } from '../models/backend-types.model'
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-group-manager',
  templateUrl: './group-manager.component.html',
  styleUrls: ['./group-manager.component.scss']
})
export class GroupManagerComponent implements OnInit, OnDestroy {

  constructor(private userData: UserDataService,
    private toastr: ToastrService) { }

  groupNames: string[];
  sampleGroups: Map<string, ISampleGroup>;
  sampleGroupsSubscription: Subscription;
  currentRenamingGroup: string;
  currentDeletingGroup: string;
  newGroupName: string;

  saveSampleGroups(): void {
    this.userData.saveSampleGroups(this.sampleGroups);
  }

  ngOnInit(): void {
    this. sampleGroupsSubscription = this.userData.sampleGroupsBehaviorSubject.subscribe(groups => {
      this.sampleGroups = groups;
      this.groupNames = Array.from(this.sampleGroups.keys()).sort();
    });
  }

  ngOnDestroy(): void {
    this.sampleGroupsSubscription.unsubscribe();
  }

  isAcceptableGroupName(name: string): boolean {
    return this.userData.isAcceptableGroupName(name);
  }

  toggleRenamingGroup(name: string): void {
    if (this.currentRenamingGroup == name) {
      this.currentRenamingGroup = undefined;
    } else {
      this.currentDeletingGroup = undefined;
      this.currentRenamingGroup = name;
    }
  }

  toggleDeletingGroup(name: string): void {
    if (this.currentDeletingGroup == name) {
      this.currentDeletingGroup= undefined;
    } else {
      this.currentRenamingGroup = undefined;
      this.currentDeletingGroup = name;
    }
  }

  submitRenamingGroup(): void {
    this.userData.renameSampleGroup(this.currentRenamingGroup, this.newGroupName);
    this.currentRenamingGroup = undefined;
    this.newGroupName = undefined;
  }

  submitDeleteGroup(): void {
    this.userData.deleteSampleGroup(this.currentDeletingGroup);
    this.toastr.success('Group name: ' + this.currentDeletingGroup, 'Sample group deleted');
    this.currentDeletingGroup = undefined;
  }
}
