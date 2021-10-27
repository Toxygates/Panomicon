import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { last, mergeMap } from 'rxjs/operators';
import { AdminDataService } from '../services/admin-data';
import { BackendService } from '../services/backend.service';

@Component({
  selector: 'app-batches',
  templateUrl: './batches.component.html',
  styleUrls: ['./batches.component.scss']
})
export class BatchesComponent  {

  constructor(public adminData: AdminDataService,
    private router: Router,
    private backend: BackendService) { }

  deleteBatch(id: string): void {
    void this.router.navigate(['/admin/progress']);
    this.backend.deleteBatch(id).pipe(
      mergeMap(() => this.adminData.startTrackingProgress()),
      last()
    ).subscribe(_res => {
      this.adminData.refreshBatches();
    });
  }

}
