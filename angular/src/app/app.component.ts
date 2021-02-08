import { Component, OnInit } from '@angular/core';
import { UserDataService } from './user-data.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {

  constructor(private userData: UserDataService) {}

  navbarIsCollapsed = true;
  enabledSampleGroupsExist: boolean;

  ngOnInit(): void {
    this.userData.enabledGroupsBehaviorSubject.subscribe(groups => {
      this.enabledSampleGroupsExist = groups.length > 0;
    });
  }
}
