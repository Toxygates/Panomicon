import { Component } from '@angular/core';
import { AdminDataService } from '../services/admin-data';

@Component({
  selector: 'app-datasets',
  templateUrl: './datasets.component.html',
  styleUrls: ['./datasets.component.scss']
})
export class DatasetsComponent  {

  constructor(public adminData: AdminDataService) { }

}
