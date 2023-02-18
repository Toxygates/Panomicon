import { Component } from '@angular/core';
import { AdminDataService } from '../services/admin-data';

@Component({
  selector: 'app-task-progress',
  templateUrl: './task-progress.component.html',
  styleUrls: ['./task-progress.component.scss'],
})
export class TaskProgressComponent {
  constructor(public adminData: AdminDataService) {}
}
