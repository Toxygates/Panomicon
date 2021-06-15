import { Component, ViewChild, OnChanges, SimpleChanges, Input, 
         AfterViewInit, NgZone, ChangeDetectorRef, TemplateRef, ElementRef } from '@angular/core';
import Tabulator from 'tabulator-tables';
import { ToastrService } from 'ngx-toastr';
import { BackendService } from '../../backend.service';
import { UserDataService } from '../../user-data.service';
import { BsModalRef, BsModalService } from 'ngx-bootstrap/modal';
import { SampleFilter } from '../../models/sample-filter.model';
import { IAttribute, Sample } from 'src/app/models/backend-types.model';
import { SampleTableHelper } from './sample-table-helper'
@Component({
  selector: 'app-sample-table',
  templateUrl: './sample-table.component.html',
  styleUrls: ['./sample-table.component.scss']
})
export class SampleTableComponent implements OnChanges, AfterViewInit {

  constructor(private backend: BackendService, private ngZone: NgZone,
    private changeDetector: ChangeDetectorRef,
    private userData: UserDataService, private toastr: ToastrService,
    private modalService: BsModalService) {
    this.requiredAttributes.add("sample_id");
  }

  tabulator: Tabulator | undefined;
  sampleFilteringModalRef: BsModalRef | undefined;
  tabulatorReady = false;

  helper = new SampleTableHelper();

  @Input() samples: Sample[] | undefined;
  @Input() batchId: string | undefined;

  samplesMap = new Map<string, Sample>()

  attributes: IAttribute[] | undefined;
  attributeMap = new Map<string, IAttribute>();
  requiredAttributes = new Set<string>();
  fetchedAttributes = new Set<IAttribute>();

  sampleFilters: SampleFilter[] = [];
  filteredSamples: Sample[] | undefined;

  sampleGroupName: string | undefined;
  sampleCreationIsCollapsed = true;
  readyToCreateGroup = true;

  controlGroupsExpanded = true;
  treatmentGroupsExpanded = true;

  selectedTreatmentGroups = new Set<string>();

  @ViewChild('tabulatorContainer') tabulatorContainer: ElementRef | undefined;

  ngOnChanges(changes: SimpleChanges):void {
    if (changes.samples != null) {
      if (changes.samples.currentValue == null) {
        if (this.tabulatorContainer != null) {
          (this.tabulatorContainer.nativeElement as HTMLElement).innerHTML = '';
        }
      } else {
        this.fetchedAttributes = new Set<IAttribute>();
        this.samplesMap = new Map<string, Sample>();
        this.sampleFilters = [];
        this.samples?.forEach((sample) => {
          this.samplesMap.set(sample.sample_id, sample);
          Object.keys(sample).forEach((attributeId) => {
            const found = this.attributeMap.get(attributeId);
            if (!found) throw new Error(`Sample had unknown attribute ${attributeId}`);
            this.fetchedAttributes.add(found);
          })
        });
        this.tryDrawTable();
      }
    }
    if (changes.batchId != null && 
        changes.batchId.currentValue != changes.batchId.previousValue) {
      if (this.batchId) { // (always true)
        this.backend.getAttributesForBatch(this.batchId)
          .subscribe(
            result => {
              this.attributes = result;
              this.attributeMap = new Map<string, IAttribute>();
              this.attributes.forEach(a => this.attributeMap.set(a.id, a));
            }
          )
      }
    }
  }

  ngAfterViewInit(): void {
    this.tabulatorReady = true;
    this.tryDrawTable();
  }

  initialColumns(): Tabulator.ColumnDefinition[] {
    return [
      //{formatter:"rowSelection", titleFormatter:"rowSelection", align:"center", headerSort:false},
      {title: 'Sample ID', field: 'sample_id'},
    ];
  }

  saveSampleGroup(): void {
    if (this.sampleGroupName && this.selectedTreatmentGroups.size > 0) {
      const samplesInGroup: string[] = [];
      this.samples?.forEach((sample) => {
        if (this.selectedTreatmentGroups.has(sample.treatment)) {
          samplesInGroup.push(sample.sample_id);
        }
      });

      this.userData.saveSampleGroup(this.sampleGroupName, samplesInGroup);
      this.toastr.success('Group name: ' + this.sampleGroupName, 'Sample group saved');
      this.sampleCreationIsCollapsed = true;
      this.sampleGroupName = undefined;
      this.selectedTreatmentGroups.clear();

      this.tabulator?.redraw();
    }
  }

  toggleControlGroups(): void {
    this.controlGroupsExpanded = !this.controlGroupsExpanded;
    const groups = this.tabulator?.getGroups();
    if (!groups) throw new Error("groups is not defined");
    groups.forEach(function(group) {
      group.toggle();
    });
  }

  toggleTreatmentGroups(): void {
    this.treatmentGroupsExpanded = !this.treatmentGroupsExpanded;
    const groups = this.tabulator?.getGroups();
    if (!groups) throw new Error("groups is not defined");
    groups.forEach(function(group) {
      group.getSubGroups().forEach(function(subGroup) {
        subGroup.toggle();
      });
    });
  }

  toggleColumn(attribute: IAttribute): void {
    const columnDefinition = this.findColumnForAttribute(attribute);
    if (columnDefinition?.field) {
      void this.tabulator?.deleteColumn(columnDefinition.field);
    } else {
      void this.tabulator?.addColumn(SampleTableHelper.createColumnForAttribute(attribute, this.sampleFilters));
      if (!this.fetchedAttributes.has(attribute)) {
        this.samples?.forEach(sample => sample[attribute.id] = "Loading...");
        void this.tabulator?.replaceData(this.samples);
        if (this.batchId && this.samples) {
          this.backend.getAttributeValues(this.samples.map(sample => sample.sample_id),
            [this.batchId], [attribute.id]).subscribe(
              result => {
                  this.fetchedAttributes.add(attribute);
                  result.forEach((element) => {
                    const sample = this.samplesMap.get(element.sample_id);
                    if (sample) {
                      sample[attribute.id] = element[attribute.id]
                    }
                  });
                  this.samples?.forEach(function(sample) {
                    if (sample[attribute.id] == "Loading...") {
                      sample[attribute.id] = "n/a";
                    }
                  })
                  void this.tabulator?.replaceData(this.samples);
              }
            );
          }
      }
    }
  }

  openSampleFilteringModal(template: TemplateRef<unknown>): void {
    this.sampleFilteringModalRef = this.modalService.show(template,
      { class: 'modal-dialog-centered modal-lg',
        ignoreBackdropClick: true });
  }

  onSubmitFilters(filters: SampleFilter[]): void {
    this.sampleFilteringModalRef?.hide();
    this.sampleFilters = filters;
    this.filterSamples(true);
    SampleTableHelper.updateColumns(this.tabulator, this.sampleFilters);
  }

  onCancelEditFilters(): void {
    this.sampleFilteringModalRef?.hide();
  }

  clearFilters(): void {
    this.sampleFilters = [];
    this.filteredSamples = undefined;
    this.tabulator?.setData(this.samples);
    SampleTableHelper.updateColumns(this.tabulator, this.sampleFilters);
  }

  private filterSamples(grouped: boolean): void {
    if (this.samples == undefined) throw new Error("samples not defined");
    const [filteredSamples, samplesForTabulator] =
      SampleTableHelper.filterSamples(this.samples, this.sampleFilters, grouped);
    this.filteredSamples = filteredSamples;
    this.tabulator?.setData(samplesForTabulator);
  }

  findColumnForAttribute(attribute: IAttribute):
      Tabulator.ColumnDefinition | undefined {
    const columnDefinitions = this.tabulator?.getColumnDefinitions();
    if (!columnDefinitions) throw new Error("columnDefinitions not defiend");
    return columnDefinitions.find(function(column) {
      return column.field == attribute.id;
    })
  }

  private tryDrawTable(): void {
    if (this.tabulatorReady && this.samples != null) {
      const tabulatorElement = document.createElement('div');
      tabulatorElement.style.width = "auto";
      (this.tabulatorContainer?.nativeElement as HTMLElement).appendChild(tabulatorElement);

      this.ngZone.runOutsideAngular(() => {
        this.tabulator = new Tabulator(tabulatorElement, {
          data: this.samples,
          selectable: true,
          columns: this.initialColumns(),
          layout:"fitDataFill",
          height: "calc(100vh - 18.3rem)",
          /* eslint-disable @typescript-eslint/no-unsafe-assignment,
                            @typescript-eslint/no-explicit-any */
          groupBy: ([function(data: { control_treatment: string }): string {
              return data.control_treatment;
            },
            function(data: { treatment: string }): string {
              return data.treatment;
            }
          // Workaround for GroupArg union type not including ((data: any) => any)[]
          ]) as any,
          /* eslint-enable @typescript-eslint/no-unsafe-assignment,
                           @typescript-eslint/no-explicit-any */
          groupHeader: SampleTableHelper.groupHeader(this.selectedTreatmentGroups),
          groupClick: (e, group)=> {
            if (e.target instanceof Element &&
                (e.target.tagName=="BUTTON" ||
                 (e.target.parentNode instanceof Element) &&
                  ((e.target.parentNode.tagName=="BUTTON")))) {
              // click is on the button
              if (this.selectedTreatmentGroups.has(group.getKey())) {
                this.selectedTreatmentGroups.delete(group.getKey());
              } else {
                this.selectedTreatmentGroups.add(group.getKey());
              }
              this.changeDetector.detectChanges();
              this.tabulator?.redraw();
            } else {
              // click is elsewhere on the header
              if (group.isVisible()) {
                group.hide();
              } else {
                group.show();
              }
            }
          },
          groupToggleElement: false,
        });
      });
    }
  }
}
