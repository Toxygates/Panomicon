import { Component } from '@angular/core';
import { AdminDataService } from '../services/admin-data';
import { BackendService } from '../services/backend.service';

@Component({
  selector: 'app-instances',
  templateUrl: './instances.component.html',
  styleUrls: ['./instances.component.scss']
})
export class InstancesComponent {

  constructor(public adminData: AdminDataService,
    private backend: BackendService) { }

  deleteInstance(id: string): void {
    this.backend.deleteInstance(id).subscribe(_res => {
      this.adminData.refreshInstances();
    });
  }

}
