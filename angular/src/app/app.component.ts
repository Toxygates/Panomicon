import { Component, OnInit } from '@angular/core';
import { catchError } from 'rxjs/operators';
import { BackendService } from './shared/services/backend.service';
import { environment } from 'src/environments/environment';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {
  constructor(
    private backend: BackendService,
  ) { }

  ngOnInit(): void {
    this.backend.getRoles()
      .pipe(
        catchError((error, _caught) => {
          window.location.href = environment.apiUrl + 'login';
          throw error;
        })
      )
      .subscribe() // empty subscription to trigger API call
  }
}
