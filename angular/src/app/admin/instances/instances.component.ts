import { Component } from '@angular/core';
import { AdminDataService } from '../services/admin-data';

@Component({
  selector: 'app-instances',
  templateUrl: './instances.component.html',
  styleUrls: ['./instances.component.scss']
})
export class InstancesComponent {

  constructor(public adminData: AdminDataService) { }

}
