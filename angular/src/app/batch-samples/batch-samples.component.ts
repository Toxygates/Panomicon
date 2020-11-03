import { Component, OnInit } from '@angular/core';
import { BackendService } from '../backend.service';
import Tabulator from 'tabulator-tables';

@Component({
  selector: 'app-batch-samples',
  templateUrl: './batch-samples.component.html',
  styleUrls: ['./batch-samples.component.scss']
})
export class BatchSamplesComponent implements OnInit {

  constructor(private backend: BackendService) { }

  tabulator: Tabulator;

  samples: any;
  batchId: string;

  readyToCreateGroup: boolean = true;

  columns = [
    {formatter:"rowSelection", titleFormatter:"rowSelection", align:"center", headerSort:false},
    {title: 'Sample ID', field: 'sample_id'},
    {title: 'Type', field: 'type'},
    {title: 'Organism', field: 'organism'},
    {title: 'Test type', field: 'test_type'},
    {title: 'Organ', field: 'organ_id'},
    {title: 'Compound', field: 'compound_name'},
    {title: 'Repeat?', field: 'sin_rep_type'},
    {title: 'Dose level', field: 'dose_level'},
    {title: 'Exposure time', field: 'exposure_time'},
    {title: 'Platform ID', field: 'platform_id'},
    {title: 'Control group', field: 'control_group'}
  ]

  tab = document.createElement('div');

  ngOnInit(): void {
  }

  loadSamplesForBatch(batchId: string) {
    delete this.samples;
    this.batchId = batchId;
    this.backend.getSamplesForBatch(batchId)
      .subscribe(
        result => {
          this.samples = result;
          this.drawTable();
        }
      )
  }

  createSampleGroup() {
    var sampleIds = this.tabulator.getSelectedData().map(x => x.sample_id);
    console.log(sampleIds);
  }

  private drawTable(): void {
    var _this = this;
    this.tabulator = new Tabulator("#my-tabular-table", {
      data: this.samples,
      selectable: true,
      columns: this.columns,
      layout:"fitDataTable",
      maxHeight: "75vh",
      rowSelectionChanged: function(data, _rows) {
        _this.readyToCreateGroup = (data.length > 0);
      }
    });
  }
}
