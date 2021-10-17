import { Component } from '@angular/core';
import { AdminDataService } from '../services/admin-data';
import { BackendService } from '../services/backend.service';

@Component({
  selector: 'app-datasets',
  templateUrl: './datasets.component.html',
  styleUrls: ['./datasets.component.scss']
})
export class DatasetsComponent  {

  constructor(public adminData: AdminDataService,
    private backend: BackendService) { }

  deleteDataset(id: string): void {
    this.backend.deleteDataset(id).subscribe(_res => {
      this.adminData.refreshDatasets();
    });
  }

}
