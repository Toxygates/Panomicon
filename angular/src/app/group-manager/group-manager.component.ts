import { Component, OnInit } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { UserDataService } from '../user-data.service';
import { ISampleGroup } from '../models/frontend-types.model'
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

@Component({
  selector: 'app-group-manager',
  templateUrl: './group-manager.component.html',
  styleUrls: ['./group-manager.component.scss']
})
export class GroupManagerComponent implements OnInit {

  constructor(public userData: UserDataService,
    private toastr: ToastrService) { }

  groupNames$!: Observable<string[]>;
  sampleGroups$!: Observable<Map<string, ISampleGroup>>;
  currentRenamingGroup: string | undefined;
  currentDeletingGroup: string | undefined;
  newGroupName: string | undefined;

  saveSampleGroups(groups: Map<string, ISampleGroup>): void {
    this.userData.sampleGroups.setItems(groups);
  }

  ngOnInit(): void {
    this.sampleGroups$ = this.userData.sampleGroups.observable;
    this.groupNames$ = this.sampleGroups$.pipe(
      map(groups => {
        return Array.from(groups.keys()).sort();
      })
    )
  }

  isAcceptableGroupName(name: string | undefined): boolean {
    return name != null && !this.userData.sampleGroups.hasItem(name);
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
    if (!this.currentRenamingGroup) throw new Error("currentRenamingGroup is not defined");
    if (!this.newGroupName) throw new Error("newGroupName is not defined");
    this.userData.sampleGroups.renameItem(this.currentRenamingGroup, this.newGroupName);
    this.currentRenamingGroup = undefined;
    this.newGroupName = undefined;
  }

  submitDeleteGroup(): void {
    if (!this.currentDeletingGroup) throw new Error("currentDeletingGroup is not defined");
    this.userData.sampleGroups.deleteItem(this.currentDeletingGroup);
    this.toastr.success('Group name: ' + this.currentDeletingGroup, 'Sample group deleted');
    this.currentDeletingGroup = undefined;
  }
}
