import {
  Component,
  ViewChild,
  AfterViewInit,
  NgZone,
  ChangeDetectorRef,
  TemplateRef,
  ElementRef,
} from '@angular/core';
import Tabulator from 'tabulator-tables';
import { ToastrService } from 'ngx-toastr';
import { UserDataService } from '../../../shared/services/user-data.service';
import { BsModalRef, BsModalService } from 'ngx-bootstrap/modal';
import { SampleFilter } from '../../../shared/models/sample-filter.model';
import { Attribute, Sample } from '../../../shared/models/backend-types.model';
import { SampleTableHelper } from './sample-table-helper';
import { BehaviorSubject, concat, Observable, of, Subscription } from 'rxjs';
import { FetchedDataService } from '../../../shared/services/fetched-data.service';
import { pairwise } from 'rxjs/operators';

@Component({
  selector: 'app-sample-table',
  templateUrl: './sample-table.component.html',
  styleUrls: ['./sample-table.component.scss'],
})
export class SampleTableComponent implements AfterViewInit {
  constructor(
    public fetchedData: FetchedDataService,
    private ngZone: NgZone,
    private changeDetector: ChangeDetectorRef,
    private userData: UserDataService,
    private toastr: ToastrService,
    private modalService: BsModalService
  ) {
    this.samples$ = this.fetchedData.samples$;
    this.filteredSamples$ = this.fetchedData.filteredSamples$;
    this.sampleFilters$ = this.fetchedData.sampleFilters$;
    this.selectedBatch$ = this.userData.selectedBatch$;
    this.attributes$ = this.fetchedData.attributes$;
    this.attributeMap$ = this.fetchedData.attributeMap$;
    this.fetchedAttributes$ = this.fetchedData.fetchedAttributes$;

    this.columnDefinitions$ = this.fetchedData.columnDefinitions$;

    this.helper = new SampleTableHelper(this.sampleFilters$);
  }

  tabulator: Tabulator | undefined;
  sampleFilteringModalRef: BsModalRef | undefined;

  helper: SampleTableHelper;

  samples$: BehaviorSubject<Sample[] | null>;
  filteredSamples$: BehaviorSubject<Sample[] | null>;
  sampleFilters$: BehaviorSubject<SampleFilter[]>;
  selectedBatch$: Observable<string | null>;

  attributes$: Observable<Attribute[] | null>;
  attributeMap$: Observable<Map<string, Attribute>>;
  fetchedAttributes$: BehaviorSubject<Set<string>>;

  columnDefinitions$: BehaviorSubject<Tabulator.ColumnDefinition[]>;

  subscriptions: Subscription[] = [];

  sampleCreationIsCollapsed = true;

  controlGroupsExpanded = true;
  treatmentGroupsExpanded = true;

  selectedTreatmentGroups = new Set<string>();

  @ViewChild('tabulatorContainer') tabulatorContainer: ElementRef | undefined;

  ngAfterViewInit(): void {
    const pairwiseSamples = concat(of(null), this.filteredSamples$).pipe(
      pairwise()
    );
    this.subscriptions.push(
      pairwiseSamples.subscribe(([previous, latest]) => {
        if (latest == null) {
          if (this.tabulatorContainer != null) {
            console.log('clearing table');
            (this.tabulatorContainer.nativeElement as HTMLElement).innerHTML =
              '';
          }
        } else {
          if (previous == null) {
            console.log('drawing table');
            this.tryDrawTable();
            setTimeout(() => void this.tabulator?.setData(latest));
          } else {
            console.log('updating table');
            void this.tabulator?.setData(latest);
          }
        }
      })
    );

    const pairwiseAttributes = concat(of(null), this.fetchedAttributes$).pipe(
      pairwise()
    );
    this.subscriptions.push(
      pairwiseAttributes.subscribe(([previous, latest]) => {
        if (previous?.size && latest?.size && this.filteredSamples$.value) {
          void this.tabulator?.replaceData(this.filteredSamples$.value);
        }
      })
    );

    this.subscriptions.push(
      this.sampleFilters$.subscribe((_filters) => {
        this.helper.updateColumnFormatters(this.tabulator);
      })
    );
  }

  onSampleGroupSaved(sampleGroupName: string): void {
    if (this.selectedTreatmentGroups.size > 0) {
      if (this.samples$.value == null) throw new Error('samples not defined');

      const samplesInGroup = this.samples$.value.filter((s) =>
        this.selectedTreatmentGroups.has(s.treatment)
      );

      this.userData.saveSampleGroup(sampleGroupName, samplesInGroup);
      this.toastr.success(
        'Group name: ' + sampleGroupName,
        'Sample group saved'
      );
      this.selectedTreatmentGroups.clear();

      this.sampleCreationIsCollapsed = true;
      this.tabulator?.redraw();
    }
  }

  toggleControlGroups(): void {
    this.controlGroupsExpanded = !this.controlGroupsExpanded;
    const groups = this.tabulator?.getGroups();
    if (!groups) throw new Error('groups is not defined');
    groups.forEach(function (group) {
      group.toggle();
    });
  }

  toggleTreatmentGroups(): void {
    this.treatmentGroupsExpanded = !this.treatmentGroupsExpanded;
    const groups = this.tabulator?.getGroups();
    if (!groups) throw new Error('groups is not defined');
    groups.forEach(function (group) {
      group.getSubGroups().forEach(function (subGroup) {
        subGroup.toggle();
      });
    });
  }

  toggleColumn(attribute: Attribute): void {
    const columnDefinition = this.findColumnForAttribute(attribute);
    if (columnDefinition?.field) {
      void this.tabulator?.deleteColumn(columnDefinition.field);
    } else {
      void this.tabulator?.addColumn(
        this.helper.createColumnForAttribute(attribute)
      );
      if (!this.fetchedAttributes$.value.has(attribute.id)) {
        this.fetchedData.fetchAttribute(attribute);
      }
      if (this.filteredSamples$.value) {
        void this.tabulator?.replaceData(this.filteredSamples$.value);
      }
    }
    this.columnDefinitions$.next(this.tabulator?.getColumnDefinitions() || []);
  }

  openSampleFilteringModal(template: TemplateRef<unknown>): void {
    this.sampleFilteringModalRef = this.modalService.show(template, {
      class: 'modal-dialog-centered modal-lg',
      ignoreBackdropClick: true,
      keyboard: false,
    });
  }

  onSubmitFilters(filters: SampleFilter[]): void {
    this.sampleFilteringModalRef?.hide();
    this.fetchedData.sampleFilters$.next(filters);
  }

  onCancelEditFilters(): void {
    this.sampleFilteringModalRef?.hide();
  }

  clearFilters(): void {
    this.fetchedData.sampleFilters$.next([]);
  }

  findColumnForAttribute(
    attribute: Attribute
  ): Tabulator.ColumnDefinition | undefined {
    const columnDefinitions = this.tabulator?.getColumnDefinitions();
    if (!columnDefinitions) throw new Error('columnDefinitions not defiend');
    return columnDefinitions.find(function (column) {
      return column.field == attribute.id;
    });
  }

  private tryDrawTable(): void {
    if (this.filteredSamples$.value != null) {
      const tabulatorElement = document.createElement('div');
      tabulatorElement.style.width = 'auto';
      (this.tabulatorContainer?.nativeElement as HTMLElement).appendChild(
        tabulatorElement
      );

      this.ngZone.runOutsideAngular(() => {
        this.tabulator = new Tabulator(tabulatorElement, {
          data: [],
          selectable: true,
          columns: this.columnDefinitions$.value,
          layout: 'fitDataFill',
          height: 'calc(100vh - 17.7rem)',
          /* eslint-disable @typescript-eslint/no-unsafe-assignment,
                            @typescript-eslint/no-explicit-any */
          groupBy: [
            function (data: { control_treatment: string }): string {
              return data.control_treatment;
            },
            function (data: { treatment: string }): string {
              return data.treatment;
            },
            // Workaround for GroupArg union type not including ((data: any) => any)[]
          ] as any,
          /* eslint-enable @typescript-eslint/no-unsafe-assignment,
                           @typescript-eslint/no-explicit-any */
          groupHeader: SampleTableHelper.groupHeader(
            this.selectedTreatmentGroups
          ),
          groupClick: (e, group) => {
            if (
              e.target instanceof Element &&
              (e.target.tagName == 'BUTTON' ||
                (e.target.parentNode instanceof Element &&
                  e.target.parentNode.tagName == 'BUTTON'))
            ) {
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
