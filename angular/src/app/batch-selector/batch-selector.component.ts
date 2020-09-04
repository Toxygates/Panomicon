import { Component, OnInit, ViewChild } from '@angular/core';
import { BatchPickerComponent } from '../batch-picker/batch-picker.component';

@Component({
  selector: 'app-batch-selector',
  templateUrl: './batch-selector.component.html',
  styleUrls: ['./batch-selector.component.css']
})
export class BatchSelectorComponent implements OnInit {

  constructor() { }

  datasetChosen: boolean = false;
  @ViewChild(BatchPickerComponent) child: BatchPickerComponent;

  ngOnInit(): void {
  }

  showBatchesForDataset(datasetId: string) {
    this.datasetChosen = true;
    this.child.loadBatchesForDataset(datasetId);
  }

}
