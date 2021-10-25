import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { AdminDataService } from '../services/admin-data';
import { Platform } from '../services/admin-types';
import { BackendService } from '../services/backend.service';

@Component({
  selector: 'app-edit-platform',
  templateUrl: './edit-platform.component.html',
  styleUrls: ['./edit-platform.component.scss']
})
export class EditPlatformComponent implements OnInit {

  constructor(private router: Router,
    private route: ActivatedRoute,
    private backend: BackendService,
    public adminData: AdminDataService) { }

    addMode = false;
    platform: Partial<Platform> = {};
    platformFile: File | undefined;
    platformType: string | undefined;

    ngOnInit(): void {
      const splits = this.route.snapshot.url;
      const mode = splits[splits.findIndex(segment => segment.path === "platforms") + 1];

      this.addMode = mode.path === "add";

      if (!this.addMode) {
        const id = this.route.snapshot.paramMap.get("id");
        this.adminData.platforms$.subscribe(platforms => {
          const match = platforms?.find(p => p.id === id);
          if (match) {
            this.platform = JSON.parse(JSON.stringify(match)) as Platform;
          }
        })
      }
    }

  handleFileInput(target: EventTarget | null): void {
    const input = target as HTMLInputElement;
    if (!input.files) {
      throw new Error("No file selected for upload");
    }
    if (input.files.length > 1) {
      throw new Error("Please upload just one file");
    }
    this.platformFile = input.files?.item(0) as File;
  }

  submit(platform: Partial<Platform>): void {
    void this.router.navigate(['/admin/platforms']);
    if (!this.platformFile) {
      throw new Error("No platform file");
    }
    const observable = this.addMode ?
      this.backend.addPlatform(platform, this.platformFile, this.platformType as string) :
      this.backend.updatePlatform(platform);
    observable.subscribe(_res => {
      this.adminData.refreshBatches();
    })
  }
}
