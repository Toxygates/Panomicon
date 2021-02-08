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

  deleteGroup(groupName: string) {
    this.sampleGroups.delete(groupName);
    this.groupNames = Array.from(this.sampleGroups.keys()).sort();
    this.userData.deleteSampleGroup(groupName);
    this.toastr.success('Group name: ' + groupName, 'Sample group deleted');
  }

  saveSampleGroups() {
    this.userData.saveSampleGroups(this.sampleGroups);
  }

  ngOnInit(): void {
    this.userData.sampleGroupsBehaviorSubject.subscribe(groups => {
      this.sampleGroups = groups;
      this.groupNames = Array.from(this.sampleGroups.keys()).sort();
    });
  }
}
