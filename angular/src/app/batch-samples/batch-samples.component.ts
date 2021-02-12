import { Component, ViewChild } from '@angular/core';
import Tabulator from 'tabulator-tables';
import { ToastrService } from 'ngx-toastr';
import { BackendService } from '../backend.service';
import { UserDataService } from '../user-data.service';

@Component({
  selector: 'app-batch-samples',
  templateUrl: './batch-samples.component.html',
  styleUrls: ['./batch-samples.component.scss']
})
export class BatchSamplesComponent {

  constructor(private backend: BackendService,
    private userData: UserDataService, private toastr: ToastrService) { }

  tabulator: Tabulator;

  samples: any;
  batchId: string;

  selectedSamples: string[] = [];

  sampleGroupName: string;
  sampleCreationIsCollapsed = true;
  readyToCreateGroup: boolean = true;

  @ViewChild('tabulatorContainer') tabulatorContainer;

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

  loadSamplesForBatch(batchId: string) {
    delete this.samples;
    this.batchId = batchId;
    this.tabulatorContainer.nativeElement.innerHTML = '';
    this.backend.getSamplesForBatch(batchId)
      .subscribe(
        result => {
          this.samples = result;
          this.drawTable();
        }
      )
  }

  saveSampleGroup() {
    if (this.sampleGroupName) {
      this.userData.saveSampleGroup(this.sampleGroupName, this.selectedSamples);
      this.toastr.success('Group name: ' + this.sampleGroupName, 'Sample group saved');
      this.sampleCreationIsCollapsed = true;
      this.sampleGroupName = undefined;
      this.tabulator.deselectRow();
    }
  }

  private drawTable(): void {
    let _this = this;
    let tabulatorElement = document.createElement('div');
    tabulatorElement.style.width = "auto";
    this.tabulatorContainer.nativeElement.appendChild(tabulatorElement);
    this.tabulator = new Tabulator(tabulatorElement, {
      data: this.samples,
      selectable: true,
      columns: this.columns,
      layout:"fitDataTable",
      maxHeight: "75vh",
      rowSelectionChanged: function(data, _rows) {
        _this.readyToCreateGroup = (data.length > 0);
        _this.selectedSamples = data.map(x => x.sample_id);
      }
    });
  }
}
