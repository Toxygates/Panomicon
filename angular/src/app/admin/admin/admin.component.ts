import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { catchError } from 'rxjs/operators';
import { BackendService } from 'src/app/shared/services/backend.service';
import { environment } from 'src/environments/environment';
import { AdminDataService } from '../services/admin-data';

@Component({
  selector: 'app-admin',
  templateUrl: './admin.component.html',
  styleUrls: ['./admin.component.scss']
})
export class AdminComponent implements OnInit {

  constructor(
    private backend: BackendService,
    public adminData: AdminDataService,
    private router: Router,
  ) { }

  navbarIsCollapsed = true;
  roles: string[] | undefined;

  ngOnInit(): void {
    this.backend.getRoles()
      .pipe(
        catchError((error, _caught) => {
          window.location.href = environment.apiUrl + 'login';
          throw error;
        })
      )
      .subscribe(roles => {
        if (!roles.includes("admin")) {
          alert("Authentication failed: you do not have the admin role. Please contact an administrator.\nLogging out...");
          this.logout();
        }
        this.roles = roles;
      })
  }

  logout(): void {
    window.location.href = environment.apiUrl + 'logout';
  }
}
