import { Component } from '@angular/core';
import { AdminDataService } from '../services/admin-data';
import { BackendService } from '../services/backend.service';

@Component({
  selector: 'app-batches',
  templateUrl: './batches.component.html',
  styleUrls: ['./batches.component.scss']
})
export class BatchesComponent  {

  constructor(public adminData: AdminDataService,
    private backend: BackendService) { }

  deleteBatch(id: string): void {
    this.backend.deleteBatch(id).subscribe(_res => {
      this.adminData.refreshBatches();
    });
  }

}
