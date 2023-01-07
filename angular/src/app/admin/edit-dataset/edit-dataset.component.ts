import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AdminDataService } from '../services/admin-data';
import { Dataset } from '../services/admin-types';
import { BackendService } from '../services/backend.service';

@Component({
  selector: 'app-edit-dataset',
  templateUrl: './edit-dataset.component.html',
  styleUrls: ['./edit-dataset.component.scss'],
})
export class EditDatasetComponent implements OnInit {
  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private backend: BackendService,
    private adminData: AdminDataService
  ) {}

  addMode = false;
  dataset: Partial<Dataset> = {};

  ngOnInit(): void {
    const splits = this.route.snapshot.url;
    const mode =
      splits[splits.findIndex((segment) => segment.path === 'datasets') + 1];
    this.addMode = mode.path === 'add';

    if (!this.addMode) {
      const id = this.route.snapshot.paramMap.get('id');
      this.adminData.datasets$.subscribe((datasets) => {
        const match = datasets?.find((d) => d.id === id);
        if (match) {
          this.dataset = JSON.parse(JSON.stringify(match)) as Dataset;
        }
      });
    }
  }

  submit(dataset: Partial<Dataset>): void {
    void this.router.navigate(['/admin/datasets']);
    const observable = this.addMode
      ? this.backend.addDataset(dataset)
      : this.backend.updateDataset(dataset);
    observable.subscribe((_res) => {
      this.adminData.refreshDatasets();
    });
  }
}
