import { Component } from '@angular/core';
import { AdminDataService } from '../services/admin-data';

@Component({
  selector: 'app-batches',
  templateUrl: './batches.component.html',
  styleUrls: ['./batches.component.scss']
})
export class BatchesComponent  {

  constructor(public adminData: AdminDataService) { }

}
