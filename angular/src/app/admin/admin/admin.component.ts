import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { BackendService } from 'src/app/shared/services/backend.service';
import { FetchedDataService } from 'src/app/shared/services/fetched-data.service';
import { environment } from 'src/environments/environment';
import { AdminDataService } from '../services/admin-data';

@Component({
  selector: 'app-admin',
  templateUrl: './admin.component.html',
  styleUrls: ['./admin.component.scss'],
})
export class AdminComponent implements OnInit {
  constructor(
    private backend: BackendService,
    private fetchedData: FetchedDataService,
    public adminData: AdminDataService,
    private router: Router
  ) {}

  navbarIsCollapsed = true;

  ngOnInit(): void {
    this.fetchedData.roles$.subscribe((roles) => {
      if (!roles?.includes('admin')) {
        alert('You do not have the admin role. Switching to viewer screen.');
        void this.router.navigateByUrl('/');
      }
    });
  }

  logout(): void {
    window.location.href = environment.apiUrl + 'logout';
  }
}
