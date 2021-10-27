import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AdminDataService } from '../services/admin-data';
import { Instance } from '../services/admin-types';
import { BackendService } from '../services/backend.service';

@Component({
  selector: 'app-edit-instance',
  templateUrl: './edit-instance.component.html',
  styleUrls: ['./edit-instance.component.scss']
})
export class EditInstanceComponent implements OnInit {

  constructor(private router: Router,
    private route: ActivatedRoute,
    private backend: BackendService,
    public adminData: AdminDataService) { }

  addMode = false;
  instance: Partial<Instance> = {};

  ngOnInit(): void {
    const splits = this.route.snapshot.url;
    const mode = splits[splits.findIndex(segment => segment.path === "instances") + 1];

    this.addMode = mode.path === "add";

    if (!this.addMode) {
      const id = this.route.snapshot.paramMap.get("id");
      this.adminData.instances$.subscribe(instances => {
        const match = instances?.find(i => i.id === id);
        if (match) {
          this.instance = JSON.parse(JSON.stringify(match)) as Instance;
        }
      })
    }
  }

  submit(instance: Partial<Instance>): void {
    void this.router.navigate(['/admin/instances']);
    const observable = this.addMode ?
      this.backend.addInstance(instance) :
      this.backend.updateInstance(instance);
    observable.subscribe(_res => {
      this.adminData.refreshInstances();
    })
  }

}
