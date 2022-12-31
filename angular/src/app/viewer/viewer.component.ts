import { Component, OnInit } from '@angular/core';
import { Observable } from 'rxjs';
import { SampleGroup } from '../shared/models/frontend-types.model';
import { UserDataService } from '../shared/services/user-data.service';
import { environment } from 'src/environments/environment';

@Component({
  selector: 'app-viewer',
  templateUrl: './viewer.component.html',
  styleUrls: ['./viewer.component.scss']
})
export class ViewerComponent implements OnInit {

  constructor(private userData: UserDataService) {}

  navbarIsCollapsed = true;
  enabledSampleGroupsExist = false;

  enabledGroups$!: Observable<SampleGroup[]>;

  ngOnInit(): void {
    this.enabledGroups$ = this.userData.enabledGroups$;
  }

  logout(): void {
    window.location.href = environment.apiUrl + 'logout';
  }
}
