import { Component, OnInit } from '@angular/core';
import { Observable } from 'rxjs';
import { ISampleGroup } from './models/frontend-types.model';
import { UserDataService } from './user-data.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {

  constructor(private userData: UserDataService) {}

  navbarIsCollapsed = true;
  enabledSampleGroupsExist = false;

  enabledGroups$!: Observable<ISampleGroup[]>;

  ngOnInit(): void {
    this.enabledGroups$ = this.userData.enabledGroups$;
  }
}
