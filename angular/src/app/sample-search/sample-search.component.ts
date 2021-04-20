import { Component, ViewChild, OnChanges, SimpleChanges, Input, 
         AfterViewInit, NgZone } from '@angular/core';
import Tabulator from 'tabulator-tables';
import { ToastrService } from 'ngx-toastr';
import { BackendService } from '../backend.service';
import { UserDataService } from '../user-data.service';

@Component({
  selector: 'app-sample-search',
  templateUrl: './sample-search.component.html',
  styleUrls: ['./sample-search.component.scss']
})
export class SampleSearchComponent implements OnChanges, AfterViewInit {

  constructor(private backend: BackendService, private ngZone: NgZone,
    private userData: UserDataService, private toastr: ToastrService) { }

  tabulator: Tabulator;
  tabulatorReady = false;

  @Input() samples: any;
  @Input() batchId: string;

  attributes: any;

  selectedSamples: string[] = [];

  sampleGroupName: string;
  sampleCreationIsCollapsed = true;
  readyToCreateGroup: boolean = true;

  controlGroupsExpanded = true;
  treatmentGroupsExpanded = true;

  static readonly controlGroupText = "Control group - ";
  static readonly treatmentGroupText = "Treatment group - ";

  @ViewChild('tabulatorContainer') tabulatorContainer;

  ngOnChanges(changes: SimpleChanges) {
    if (changes.samples != null) {
      if (changes.samples.currentValue == null && this.tabulatorContainer != null) {
        this.tabulatorContainer.nativeElement.innerHTML = '';
      } else {
        this.tryDrawTable();
      }
    }
    if (changes.batchId != null && 
        changes.batchId.currentValue != changes.batchId.previousValue) {
      this.backend.getAttributesForBatch(this.batchId)
        .subscribe(
          result => {
            this.attributes = result;
          }
        )

    }
  }

  ngAfterViewInit() {
    this.tabulatorReady = true;
    this.tryDrawTable();
  }

  columns = [
    //{formatter:"rowSelection", titleFormatter:"rowSelection", align:"center", headerSort:false},
    {title: 'Sample ID', field: 'sample_id'},
  ]

  tab = document.createElement('div');

  saveSampleGroup() {
    if (this.sampleGroupName) {
      this.userData.saveSampleGroup(this.sampleGroupName, this.selectedSamples);
      this.toastr.success('Group name: ' + this.sampleGroupName, 'Sample group saved');
      this.sampleCreationIsCollapsed = true;
      this.sampleGroupName = undefined;
      this.tabulator.deselectRow();
    }
  }

  toggleControlGroups() {
    let newState = this.controlGroupsExpanded = !this.controlGroupsExpanded;
    let groups = this.tabulator.getGroups();
    groups.forEach(function(group) {
      if (newState) {
        group.show();
      } else {
        group.hide();
      }
    });
  }

  toggleTreatmentGroups() {
    let newState = this.treatmentGroupsExpanded = !this.treatmentGroupsExpanded;
    let groups = this.tabulator.getGroups();
    groups.forEach(function(group) {
      group.getSubGroups().forEach(function(subGroup) {
        if (newState) {
          subGroup.show();
        } else {
          subGroup.hide();
        }
      });
    });
  }

  toggleColumn(attribute: any) {
    let columnDefinition = this.columnForAttribute(attribute);
    if (columnDefinition != null) {
      this.tabulator.deleteColumn(columnDefinition.field);
    } else {
      this.tabulator.addColumn({
        title: attribute.title,
        field: attribute.id,
      });
    }
  }

  columnForAttribute(attribute: any) {
    let columnDefinitions = this.tabulator.getColumnDefinitions();
    let column = columnDefinitions.find(function(column) {
      return column.field == attribute.id;
    })
    return column;
  }

  private tryDrawTable(): void {
    if (this.tabulatorReady && this.samples != null) {
      let _this = this;
      let tabulatorElement = document.createElement('div');
      tabulatorElement.style.width = "auto";
      this.tabulatorContainer.nativeElement.appendChild(tabulatorElement);
      this.ngZone.runOutsideAngular(() => {
        this.tabulator = new Tabulator(tabulatorElement, {
          data: this.samples,
          selectable: true,
          columns: this.columns,
          layout:"fitDataFill",
          maxHeight: "75vh",
          groupBy: [function(data) {
              return SampleSearchComponent.controlGroupText + data.control_treatment;
            },
            function(data) {
              return SampleSearchComponent.treatmentGroupText + data.treatment;
            }
          ],
          groupToggleElement: "header",
          // groupStartOpen: function(value, count, data, group){
          //   return true;
          //   return value.substring(0, 7) == "Control";
          // },
          rowFormatter:function(row){
            var data = row.getData(); //get data object for row
          },
        });
      });
    }
  }
}
