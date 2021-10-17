import { Component, OnInit } from '@angular/core';
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
    public adminData: AdminDataService
  ) { }

  navbarIsCollapsed = true;
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

  handleFileInput(target: EventTarget | null): void {
    const input = target as HTMLInputElement;
    const file = input.files?.item(0);
    if (!file) {
      throw new Error("No file selected for upload");
    }
    this.backend.uploadFile(file).subscribe(data => {
      console.log(data);
      // do something, if upload success
      }, error => {
        console.log(error);
      });
  }

  deleteBatch(batchId: string): void {
    console.log("deleting batch " + batchId);
    this.backend.deleteBatch(batchId)
      .subscribe(result => {
        console.log(result);
      });
  }
}
