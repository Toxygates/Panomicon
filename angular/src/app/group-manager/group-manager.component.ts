import { Component, OnInit } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { UserDataService } from '../user-data.service';

@Component({
  selector: 'app-group-manager',
  templateUrl: './group-manager.component.html',
  styleUrls: ['./group-manager.component.scss']
})
export class GroupManagerComponent implements OnInit {

  constructor(private userData: UserDataService,
    private toastr: ToastrService) { }

  groupNames: string[];
  sampleGroups: Map<string, string[]>;

  deleteGroup(groupName: string) {
    this.sampleGroups.delete(groupName);
    this.groupNames = Array.from(this.sampleGroups.keys()).sort();
    this.userData.deleteSampleGroup(groupName);
    this.toastr.success('Group name: ' + groupName, 'Sample group deleted');
  }

  ngOnInit(): void {
    this.sampleGroups = this.userData.getSampleGroups();
    this.groupNames = Array.from(this.sampleGroups.keys()).sort();
  }
}
