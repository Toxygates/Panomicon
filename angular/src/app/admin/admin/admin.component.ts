import { Component, OnInit } from '@angular/core';
import { catchError } from 'rxjs/operators';
import { BackendService } from 'src/app/viewer/shared/services/backend.service';
import { environment } from 'src/environments/environment';

@Component({
  selector: 'app-admin',
  templateUrl: './admin.component.html',
  styleUrls: ['./admin.component.scss']
})
export class AdminComponent implements OnInit {

  constructor(private backend: BackendService) { }

  roles: string[] | undefined;

  ngOnInit(): void {
    this.backend.getRoles()
      .pipe(
        catchError((error, _caught) => {
          window.location.href = environment.apiUrl +'login';
          throw error;
        })
      )
      .subscribe(roles => {
        if (roles.length == 0) {
          window.location.href = environment.apiUrl +'login';
        }
        this.roles = roles;
      })
  }

}
