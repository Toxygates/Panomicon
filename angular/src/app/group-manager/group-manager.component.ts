import { Component, OnInit } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { UserDataService } from '../user-data.service';
import { ISampleGroup } from '../models/sample-group.model'

@Component({
  selector: 'app-group-manager',
  templateUrl: './group-manager.component.html',
  styleUrls: ['./group-manager.component.scss']
})
export class GroupManagerComponent implements OnInit {

  constructor(private userData: UserDataService,
    private toastr: ToastrService) { }

  groupNames: string[];
  sampleGroups: Map<string, ISampleGroup>;
  currentRenamingGroup: string;
  currentDeletingGroup: string;
  newGroupName: string;

  saveSampleGroups() {
    this.userData.saveSampleGroups(this.sampleGroups);
  }

  ngOnInit(): void {
    this.userData.sampleGroupsBehaviorSubject.subscribe(groups => {
      this.sampleGroups = groups;
      this.groupNames = Array.from(this.sampleGroups.keys()).sort();
    });
  }

  isAcceptableGroupName(name: string) {
    return this.userData.isAcceptableGroupName(name);
  }

  toggleRenamingGroup(name: string) {
    if (this.currentRenamingGroup == name) {
      this.currentRenamingGroup = undefined;
    } else {
      this.currentDeletingGroup = undefined;
      this.currentRenamingGroup = name;
    }
  }

  toggleDeletingGroup(name: string) {
    if (this.currentDeletingGroup == name) {
      this.currentDeletingGroup= undefined;
    } else {
      this.currentRenamingGroup = undefined;
      this.currentDeletingGroup = name;
    }
  }

  submitRenamingGroup() {
    this.userData.renameSampleGroup(this.currentRenamingGroup, this.newGroupName);
    this.currentRenamingGroup = undefined;
    this.newGroupName = undefined;
  }

  submitDeleteGroup() {
    this.userData.deleteSampleGroup(this.currentDeletingGroup);
    this.toastr.success('Group name: ' + this.currentDeletingGroup, 'Sample group deleted');
    this.currentDeletingGroup = undefined;
  }
}
