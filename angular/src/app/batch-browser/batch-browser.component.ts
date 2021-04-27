import { Component, ChangeDetectorRef } from '@angular/core';
import { BackendService } from '../backend.service';

@Component({
  selector: 'app-batch-browser',
  templateUrl: './batch-browser.component.html',
  styleUrls: ['./batch-browser.component.scss']
})
export class BatchBrowserComponent {

  constructor(private backend: BackendService, 
    private changeDetector: ChangeDetectorRef) { }

  datasetId: string = "otg";
  batchId: string;
  samples: any;

  batchChanged(batchId: string) {
    this.batchId = batchId;
    delete this.samples;
    this.backend.getSamplesForBatch(batchId)
      .subscribe(
        result => {
          this.samples = result;
        }
      )
  }
}
