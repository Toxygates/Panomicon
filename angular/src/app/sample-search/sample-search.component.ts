import { Component, ViewChild, OnChanges, SimpleChanges, Input, 
         AfterViewInit, NgZone, ChangeDetectorRef } from '@angular/core';
import Tabulator from 'tabulator-tables';
import { ToastrService } from 'ngx-toastr';
import { BackendService } from '../backend.service';
import { UserDataService } from '../user-data.service';
import { BsModalRef, BsModalService } from 'ngx-bootstrap/modal';
import { SampleFilter } from '../models/sample-filter.model';

@Component({
  selector: 'app-sample-search',
  templateUrl: './sample-search.component.html',
  styleUrls: ['./sample-search.component.scss']
})
export class SampleSearchComponent implements OnChanges, AfterViewInit {

  constructor(private backend: BackendService, private ngZone: NgZone,
    private changeDetector: ChangeDetectorRef,
    private userData: UserDataService, private toastr: ToastrService,
    private modalService: BsModalService) {
    this.requiredAttributes.add("sample_id");
  }

  tabulator: Tabulator;
  modalRef: BsModalRef;
  tabulatorReady = false;
  @ViewChild('sampleFilteringModal') sampleFilteringTemplate;

  @Input() samples: any[];
  @Input() batchId: string;

  samplesMap: Map<string, any>;

  attributes: any;
  requiredAttributes = new Set<string>();
  fetchedAttributes: Set<string>;

  sampleFilters: SampleFilter[] = [];

  sampleGroupName: string;
  sampleCreationIsCollapsed = true;
  readyToCreateGroup: boolean = true;

  controlGroupsExpanded = true;
  treatmentGroupsExpanded = true;

  selectedGroups = new Set<string>();

  @ViewChild('tabulatorContainer') tabulatorContainer;

  ngOnChanges(changes: SimpleChanges) {
    if (changes.samples != null) {
      if (changes.samples.currentValue == null) {
        if (this.tabulatorContainer != null) {
          this.tabulatorContainer.nativeElement.innerHTML = '';
        }
      } else {
        this.fetchedAttributes = new Set<string>();
        this.samplesMap = new Map<string, any>();
        this.sampleFilters = [];
        let _this = this;
        this.samples.forEach(function(sample) {
          _this.samplesMap[sample.sample_id] = sample;
          Object.keys(sample).forEach(function(attribute) {
            _this.fetchedAttributes.add(attribute);
          })
        });
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
    if (this.sampleGroupName && this.selectedGroups.size > 0) {
      let samplesInGroup = [];
      let _this = this;
      this.selectedGroups.forEach(function(group) {
        console.log("group name " + group);
      })
      this.samples.forEach(function(sample) {
        console.log("trying " + sample.treatment);
        if (_this.selectedGroups.has(sample.treatment)) {
          samplesInGroup.push(sample.sample_id);
        }
      });

      this.userData.saveSampleGroup(this.sampleGroupName, samplesInGroup);
      this.toastr.success('Group name: ' + this.sampleGroupName, 'Sample group saved');
      this.sampleCreationIsCollapsed = true;
      this.sampleGroupName = undefined;
      this.selectedGroups.clear();

      this.tabulator.redraw();
    }
  }

  toggleControlGroups() {
    this.controlGroupsExpanded = !this.controlGroupsExpanded;
    let groups = this.tabulator.getGroups();
    groups.forEach(function(group) {
      group.toggle();
    });
  }

  toggleTreatmentGroups() {
    this.treatmentGroupsExpanded = !this.treatmentGroupsExpanded;
    let groups = this.tabulator.getGroups();
    groups.forEach(function(group) {
      group.getSubGroups().forEach(function(subGroup) {
        subGroup.toggle();
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
      if (!this.fetchedAttributes.has(attribute.id)) {
        let _this = this;
        this.samples.forEach(sample => sample[attribute.id] = "Loading...");
        this.tabulator.replaceData(_this.samples);
        this.backend.getAttributeValues(this.samples.map(sample => sample.sample_id),
          [this.batchId], [attribute.id]).subscribe(
            result => {
                this.fetchedAttributes.add(attribute.id);
                result.forEach(function(element) {
                  _this.samplesMap[element.sample_id][attribute.id] = element[attribute.id]
                });
                this.samples.forEach(function(sample) {
                  if (sample[attribute.id] == "Loading...") {
                    sample[attribute.id] = "n/a";
                  }
                })
                this.tabulator.replaceData(_this.samples);
            }
          );
      }
    }
  }

  openSampleFilteringModal() {
    this.modalRef = this.modalService.show(this.sampleFilteringTemplate,
      { class: 'modal-dialog-centered ' });
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

      let groupHeader = function(value, count, data, group) {
        //value - the value all members of this group share
        //count - the number of rows in this group
        //data - an array of all the row data objects in this group
        //group - the group component for the group

        let prefix, itemCount, itemWord, button;

        if (group.getParentGroup()) {
          itemCount = count;
          itemWord = " sample";
          if (value != group.getParentGroup().getKey()) {
            prefix = "Treatment group - ";
            if (_this.selectedGroups.has(value)) {
              button = "<button type='button' class='btn btn-success'>"
                + "Group selected <i class='bi bi-check'></i></button>"
            } else {
              button = "<button type='button' class='btn btn-secondary'>"
              + "Select group</button>"
            }
          } else {
            prefix = "Control group - ";
            button = "";
          }
        } else {
          prefix = "Control group - ";
          itemCount = group.getSubGroups().length;
          itemWord = " group";
          button = "";
        }

        itemWord += itemCount != 1 ? "s" : "";

        return prefix + value + "<span>(" + itemCount + itemWord + ")</span> " +
           button;
      }

      this.ngZone.runOutsideAngular(() => {
        this.tabulator = new Tabulator(tabulatorElement, {
          data: this.samples,
          selectable: true,
          columns: this.columns,
          layout:"fitDataFill",
          height: "calc(100vh - 18.3rem)",
          groupBy: [function(data) {
              return data.control_treatment;
            },
            function(data) {
              return data.treatment;
            }
          ],
          groupHeader: groupHeader,
          groupClick:function(e, group){
            if (e.target.tagName=="BUTTON" || 
                e.target.parentNode.tagName=="BUTTON") {
              // click is on the button
              if (_this.selectedGroups.has(group.getKey())) {
                _this.selectedGroups.delete(group.getKey());
              } else {
                _this.selectedGroups.add(group.getKey());
              }
              _this.changeDetector.detectChanges();
              _this.tabulator.redraw();
            } else {
              // click is elsewhere on the header
              if (group.getVisibility()) {
                group.hide();
              } else {
                group.show();
              }
            }
          },
          groupToggleElement: false,
          // groupStartOpen: function(value, count, data, group){
          //   return true;
          //   return value.substring(0, 7) == "Control";
          // },
        });
      });
    }
  }
}
