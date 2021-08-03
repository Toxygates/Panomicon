import { Component, OnInit } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { renameItem, UserDataService } from '../shared/services/user-data.service';
import { SampleGroup } from '../shared/models/frontend-types.model'
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
  sampleGroups$!: Observable<Map<string, SampleGroup>>;
  currentRenamingGroup: string | undefined;
  currentDeletingGroup: string | undefined;
  newGroupName: string | undefined;

  saveSampleGroups(groups: Map<string, SampleGroup>): void {
    this.userData.sampleGroups$.next(groups);
  }

  ngOnInit(): void {
    this.sampleGroups$ = this.userData.sampleGroups$;
    this.groupNames$ = this.sampleGroups$.pipe(
      map(groups => {
        return Array.from(groups.keys()).sort();
      })
    )
  }

  isAcceptableGroupName(name: string | undefined): boolean {
    return name != null && !this.userData.sampleGroups$.value.has(name);
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
    renameItem(this.userData.sampleGroups$.value, this.currentRenamingGroup, this.newGroupName);
    this.userData.sampleGroups$.next(this.userData.sampleGroups$.value);
    this.currentRenamingGroup = undefined;
    this.newGroupName = undefined;
  }

  submitDeleteGroup(): void {
    if (!this.currentDeletingGroup) throw new Error("currentDeletingGroup is not defined");
    this.userData.sampleGroups$.value.delete(this.currentDeletingGroup);
    this.userData.sampleGroups$.next(this.userData.sampleGroups$.value);
    this.toastr.success('Group name: ' + this.currentDeletingGroup, 'Sample group deleted');
    this.currentDeletingGroup = undefined;
  }
}
