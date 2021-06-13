import { AfterViewInit, OnInit, ChangeDetectorRef, Component, HostListener, ViewChild, OnDestroy, ElementRef, TemplateRef } from '@angular/core';
import { BsModalService, BsModalRef } from 'ngx-bootstrap/modal';
import { ActivatedRoute, Router } from '@angular/router';
import { UserDataService } from '../user-data.service';
import { ISampleGroup } from '../models/backend-types.model'
import Tabulator from 'tabulator-tables';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-expression-table',
  templateUrl: './expression-table.component.html',
  styleUrls: ['./expression-table.component.scss']
})
export class ExpressionTableComponent implements OnInit, AfterViewInit,
  OnDestroy {

  constructor(private userData: UserDataService,
    private activatedRoute: ActivatedRoute,
    private router: Router, private modalService: BsModalService,
    private changeDetector: ChangeDetectorRef) { }

  tabulator: Tabulator | undefined;
  modalRef: BsModalRef | undefined;
  @ViewChild('tabulatorContainer') tabulatorContainer!: ElementRef;
  @ViewChild('gotoPageModal') gotoPageTemplate!: TemplateRef<unknown>;

  enabledSampleGroups: ISampleGroup[] = [];
  enabledSampleGroupsSubscription: Subscription | undefined;
  dataFetched = false;
  lastPage = 0;
  tablePageNumber = 0;
  goToPageSubmitEnabled = false;

  probeTitlesMutator = (_value: unknown,
      data: Record<string, string | string[]>, _type: unknown,
      _params: unknown, _component: unknown): string => {
    return (data.probeTitles as string[]).join(" / ");
  }

  geneSymbolsMutator = (_value: unknown,
      data: Record<string, string | string[]>, _type: unknown,
      _params: unknown, _component: unknown): string => {
    return (data.geneSymbols as string[]).join(" / ");
  }

  log2foldMutator = (value: string, _data: unknown, _type: unknown,
    _params: unknown, _component: unknown): string => {
    return Number(value).toFixed(3);
  }

  pValueMutator = (value: string, _data: unknown, _type: unknown,
    _params: unknown, _component: unknown): string => {
    const numericalValue = Number(value)
    if (numericalValue > .00005) {
      return numericalValue.toFixed(4);
    } else {
      return numericalValue.toExponential(3);
    }
  }

  columns: Tabulator.ColumnDefinition[] = [
    {title: 'Gene symbols', field: 'geneSymbols',
      mutator: this.geneSymbolsMutator, headerSort:false, width:"15rem"},
    {title: 'Probe titles', field: 'probeTitles',
      mutator: this.probeTitlesMutator, headerSort:false, width:"50rem"},
    {title: 'Probe', field: 'probe', headerSort:false, width:"15rem"},
  ]

  ngOnInit(): void {
    this.enabledSampleGroupsSubscription = this.userData.enabledGroupsBehaviorSubject.subscribe(enabledGroups => {
      this.enabledSampleGroups = enabledGroups;
      if (enabledGroups.length == 0) {
        void this.router.navigate(['']);
      } else {
        for (const group of enabledGroups) {
          this.columns.push({title: group.name, field: group.name,
            headerSort: true, mutator: this.log2foldMutator,
            headerSortStartingDir:'desc', width:"15rem"});
          this.columns.push({title: group.name + ' (p)', field: group.name + '(p)',
            headerSort: true, mutator: this.log2foldMutator,
            headerSortStartingDir:'desc', width:"15rem"});
        }
      }
    });
  }

  ngAfterViewInit(): void {
    this.drawTable();
  }

  ngOnDestroy(): void {
    this.enabledSampleGroupsSubscription?.unsubscribe();
  }

  private drawTable(): void {
    const tabulatorElement = document.createElement('div');
    tabulatorElement.style.width = "auto";
    (this.tabulatorContainer.nativeElement as HTMLElement).appendChild(tabulatorElement);
    this.tabulator = new Tabulator(tabulatorElement, {
      pagination:"remote",
      ajaxURL: "json/matrix",
      ajaxConfig:"POST",
      ajaxContentType: {
        headers: {
          'Content-Type': 'application/json',
        },
        body: (_url, _config, params: { page: number, sorters: string[] }) => {
          const groupInfoArray = [];
          for (const group of this.enabledSampleGroups) {
            groupInfoArray.push({ "name": group.name, "sampleIds": group.samples })
          }
          const requestBodyObject = {
            groups: groupInfoArray,
            page: params.page,
            sorter: params.sorters[0]
          }
          return(JSON.stringify(requestBodyObject));
        },
      },
      ajaxURLGenerator: function(url, _config, params: {page: number}){
        const page = params.page;
        return `${url}?offset=${((page - 1) * 100)}`;
      },
      paginationDataReceived: {
        "data": "rows",
      },
      dataLoaded: (function(expressionTableComponent) { return function(this: Tabulator) {
        expressionTableComponent.dataFetched = true;
        expressionTableComponent.lastPage = (this.getPageMax() as number);
        expressionTableComponent.changeDetector.detectChanges();
        expressionTableComponent.tablePageNumber = (this.getPage() as number);
        };
      })(this),
      columns: this.columns,
      index: "probe",
      layout:"fitDataTable",
      height: "calc(100vh - 8.375rem)",
      columnHeaderSortMulti:false,
      ajaxSorting:true,
      initialSort:[
        {column: this.enabledSampleGroups[0].name, dir:"desc"}
      ],
      tooltips:true,
      tooltipsHeader:true,
      ajaxLoaderLoading: "<div class=\"spinner-border text-secondary\" role=\"status\"><span class=\"sr-only\">Loading...</span></div>",
      footerElement:"<div class=\"d-none d-sm-block\" style=\"float: left;\"><button class=\"tabulator-page\" style=\"border-radius: 4px; border: 1px solid #dee2e6;\" onclick=\"window.dispatchEvent(new CustomEvent('OpenGotoPageModal'));\">Go to page...</button></div>",
    });
  }

  @HostListener("window:OpenGotoPageModal")
  onOpenGotoPageModal(): void {
    this.modalRef = this.modalService.show(this.gotoPageTemplate,
      { class: 'modal-dialog-centered' });
  }

  onSubmitGotoPageModal(): void {
    if (!this.tabulator) throw new Error("tabulator is not defined");
    void this.tabulator?.setPage(this.tablePageNumber);
    this.modalRef?.hide();
  }
}
