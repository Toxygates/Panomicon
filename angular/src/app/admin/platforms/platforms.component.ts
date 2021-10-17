import { Component, OnInit } from '@angular/core';
import { AdminDataService } from '../services/admin-data';

@Component({
  selector: 'app-platforms',
  templateUrl: './platforms.component.html',
  styleUrls: ['./platforms.component.scss']
})
export class PlatformsComponent {

  constructor(public adminData: AdminDataService) { }

}
