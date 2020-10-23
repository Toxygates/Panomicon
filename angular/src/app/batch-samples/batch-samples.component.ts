import { Component, OnInit } from '@angular/core';
import { BackendService } from '../backend.service';

@Component({
  selector: 'app-batch-samples',
  templateUrl: './batch-samples.component.html',
  styleUrls: ['./batch-samples.component.scss']
})
export class BatchSamplesComponent implements OnInit {

  constructor(private backend: BackendService) { }

  samples: any;
  batchId: string;
  virtualScroll: boolean;

  cols: any[];
  tableWidth: Number;

  ngOnInit(): void {
    this.cols = [
      {field: 'sample_id', header: 'Sample ID', width: "300"},
      {field: 'type', header: 'Type', width: "75"},
      {field: 'organism', header: 'Organism', width: "75"},
      {field: 'test_type', header: 'Test type', width: "75"},
      {field: 'organ_id', header: 'Organ', width: "75"},
      {field: 'compound_name', header: 'Compound', width: "125"},
      {field: 'sin_rep_type', header: 'Repeat?', width: "75"},
      {field: 'dose_level', header: 'Dose level', width: "75"},
      {field: 'exposure_time', header: 'Exposure time', width: "75"},
      {field: 'platform_id', header: 'Platform ID', width: "85"},
      {field: 'control_group', header: 'Control group', width: "85"}
    ];
    this.tableWidth = this.cols.reduce((sum, v) => sum + +v.width, 0) + 100;
  }

  loadSamplesForBatch(batchId: string) {
    delete this.samples;
    this.batchId = batchId;
    this.backend.getSamplesForBatch(batchId)
      .subscribe(
        result => {
          this.samples = result;
          this.virtualScroll = this.samples.length > 50;
        }
      )
  }

}
