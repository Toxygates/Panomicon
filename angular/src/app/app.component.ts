import { Component, OnInit } from '@angular/core';
import { catchError } from 'rxjs/operators';
import { environment } from 'src/environments/environment';
import { FetchedDataService } from './shared/services/fetched-data.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {
  constructor(
    private fetchedData: FetchedDataService,
  ) { }

  ngOnInit(): void {
    this.fetchedData.roles$
      .pipe(
        catchError((error, _caught) => {
          window.location.href = environment.apiUrl + 'login';
          throw error;
        })
      )
      .subscribe() // empty subscription to trigger API call
  }
}
