import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { UserDataService } from './user-data.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit, OnDestroy {

  constructor(private userData: UserDataService) {}

  navbarIsCollapsed = true;
  enabledSampleGroupsExist: boolean;

  enabledGroupsSubscription: Subscription;

  ngOnInit(): void {
    this.enabledGroupsSubscription = this.userData.enabledGroupsBehaviorSubject.subscribe(groups => {
      this.enabledSampleGroupsExist = groups.length > 0;
    });
  }

  ngOnDestroy(): void {
    this.enabledGroupsSubscription.unsubscribe();
  }
}
