import { Component, ViewChild } from '@angular/core';
import { BatchSamplesComponent } from './batch-samples/batch-samples.component';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent {
  title = 'Panomicon Angular test';

  @ViewChild(BatchSamplesComponent) batchSamples: BatchSamplesComponent;

  selectBatch(batchId:string) {
    this.batchSamples.loadSamplesForBatch(batchId);
  }
  
}