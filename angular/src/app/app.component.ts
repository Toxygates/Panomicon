import { Component, OnInit, TemplateRef, ViewChild } from '@angular/core';
import { BsModalService } from 'ngx-bootstrap/modal';
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
    private modalService: BsModalService,
  ) { }

  @ViewChild('loginModal') loginTemplate!: TemplateRef<unknown>;

  ngOnInit(): void {
    this.fetchedData.roles$
      .pipe(
        catchError((error, _caught) => {
          this.modalService.show(this.loginTemplate,
            { class: 'modal-dialog-centered' });
          throw error;
        })
      )
      .subscribe() // empty subscription to trigger API call
  }

  login(): void {
    window.location.href = environment.apiUrl + 'login';
  }


  register(): void {
    window.location.href = environment.apiUrl + 'register';
  }
}
