import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { last, mergeMap } from 'rxjs/operators';
import { AdminDataService } from '../services/admin-data';
import { BackendService } from '../services/backend.service';

@Component({
  selector: 'app-platforms',
  templateUrl: './platforms.component.html',
  styleUrls: ['./platforms.component.scss']
})
export class PlatformsComponent {

  constructor(public adminData: AdminDataService,
    private router: Router,
    private backend: BackendService) { }

  deletePlatform(id: string): void {
    void this.router.navigate(['/admin/progress']);
    this.backend.deletePlatform(id).pipe(
      mergeMap(() => this.adminData.startTrackingProgress()),
      last()
    ).subscribe(_res => {
      this.adminData.refreshPlatforms();
    });
  }

}
