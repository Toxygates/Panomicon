import { Component, OnInit } from '@angular/core';
import { Observable } from 'rxjs';
import { SampleGroup } from './shared/models/frontend-types.model';
import { UserDataService } from './shared/services/user-data.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {

  constructor(private userData: UserDataService) {}

  navbarIsCollapsed = true;
  enabledSampleGroupsExist = false;

  enabledGroups$!: Observable<SampleGroup[]>;

  ngOnInit(): void {
    this.enabledGroups$ = this.userData.enabledGroups$;
  }
}
