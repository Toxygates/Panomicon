import { Component } from '@angular/core';
import { AdminDataService } from '../services/admin-data';
import { BackendService } from '../services/backend.service';

@Component({
  selector: 'app-platforms',
  templateUrl: './platforms.component.html',
  styleUrls: ['./platforms.component.scss']
})
export class PlatformsComponent {

  constructor(public adminData: AdminDataService,
    private backend: BackendService) { }

  deletePlatform(id: string): void {
    this.backend.deletePlatform(id).subscribe(_res => {
      this.adminData.refreshPlatforms();
    });
  }

}
