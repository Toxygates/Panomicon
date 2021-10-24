import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AdminDataService } from '../services/admin-data';
import { Batch } from '../services/admin-types';
import { BackendService } from '../services/backend.service';

@Component({
  selector: 'app-edit-batch',
  templateUrl: './edit-batch.component.html',
  styleUrls: ['./edit-batch.component.scss']
})
export class EditBatchComponent implements OnInit {

  constructor(private router: Router,
    private route: ActivatedRoute,
    private backend: BackendService,
    public adminData: AdminDataService) { }

  addMode = false;
  batch: Partial<Batch> = {};
  recalculate = false;
  files = new Map<string, File>();

  ngOnInit(): void {
    const splits = this.route.snapshot.url;
    const mode = splits[splits.findIndex(segment => segment.path === "batches") + 1];
    this.addMode = mode.path === "add";

    if (!this.addMode) {
      const id = this.route.snapshot.paramMap.get("id");
      this.adminData.batches$.subscribe(batches => {
        const match = batches?.find(b => b.id === id);
        if (match) {
          this.batch = JSON.parse(JSON.stringify(match)) as Batch;
        }
      })
    }
  }

  toggleInstance(instance: string): void {
    if (!this.batch.enabledInstances) {
      this.batch.enabledInstances = [];
    }
    if (this.batch.enabledInstances.includes(instance)) {
      this.batch.enabledInstances = this.batch.enabledInstances.filter(i =>
        i !== instance);
    } else {
      this.batch.enabledInstances.push(instance);
    }
  }

  handleFileInput(target: EventTarget | null, tag: string): void {
    const input = target as HTMLInputElement;
    if (!input.files) {
      throw new Error("No file selected for upload");
    }
    if (input.files.length > 1) {
      throw new Error("Please upload just one file");
    }
    this.files.set(tag, input.files?.item(0) as File);
  }

  submit(batch: Partial<Batch>): void {
    void this.router.navigate(['/admin/batches']);
    const observable = this.addMode ?
      this.backend.addBatch(batch, this.files) :
      this.backend.updateBatch(batch, this.files,
        this.recalculate);
    observable.subscribe(_res => {
      this.adminData.refreshBatches();
    })
  }
}
