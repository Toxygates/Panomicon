import { AfterViewInit, OnInit, ChangeDetectorRef, Component, HostListener, ViewChild, OnDestroy, ElementRef, TemplateRef, NgZone } from '@angular/core';
import { BsModalService, BsModalRef } from 'ngx-bootstrap/modal';
import { ActivatedRoute, Router } from '@angular/router';
import { UserDataService } from '../../shared/services/user-data.service';
import { GeneSet, SampleGroup } from '../../shared/models/frontend-types.model'
import Tabulator from 'tabulator-tables';
import { BehaviorSubject, combineLatest, Observable, Subscription } from 'rxjs';
import { map } from 'rxjs/operators';

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
    private changeDetector: ChangeDetectorRef,
    private ngZone: NgZone) { }

  tabulator: Tabulator | undefined;
  modalRef: BsModalRef | undefined;
  @ViewChild('tabulatorContainer') tabulatorContainer!: ElementRef;
  @ViewChild('gotoPageModal') gotoPageTemplate!: TemplateRef<unknown>;

  tableData$ = new BehaviorSubject<Record<string, string>[]>([]);

  enabledSampleGroups: SampleGroup[] = [];
  enabledSampleGroupsSubscription: Subscription | undefined;
  geneSets$!: Observable<Map<string, GeneSet>>;
  geneSetNames$!: Observable<string[]>;

  currentGeneSet: string | undefined;
  // filters!: { column: string, type: string, threshold: number }[]
  probes: string[] = [];
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

  log2foldMutator = (field: string) =>
    (_value: unknown, data: {expression: Record<string, string>},
      _type: unknown, _params:unknown, _column:unknown): string => {
      const numericalValue = Number(data["expression"][field]);
      return Number(numericalValue).toFixed(3);
    }

  pValueMutator = (field: string) =>
    (_value: unknown, data: {expression: Record<string, string>},
      _type: unknown, _params:unknown, _column:unknown): string => {
      const numericalValue = Number(data["expression"][`${field}(p)`]);
      if (numericalValue > .0001) {
        return numericalValue.toFixed(4);
      } else {
        return numericalValue.toExponential(3);
      }
    }

  columns: Tabulator.ColumnDefinition[] = [
    {title: 'Gene symbols', field: 'geneSymbols',
      mutator: this.geneSymbolsMutator, headerSort:false, width:"10rem"},
    {title: 'Probe titles', field: 'probeTitles',
      mutator: this.probeTitlesMutator, headerSort:false, width:"30rem"},
    {title: 'Probe', field: 'probe', headerSort:false, width:"10rem"},
  ]

  ngOnInit(): void {
    this.enabledSampleGroupsSubscription = this.userData.enabledGroups$.subscribe(enabledGroups => {
      this.enabledSampleGroups = enabledGroups;
      if (enabledGroups.length == 0) {
        void this.router.navigate(['']);
      } else {
        for (const group of enabledGroups) {
          this.columns.push({title: group.name, field: group.name,
            mutator: this.log2foldMutator(group.name), headerSort: true,
            headerSortStartingDir:'desc', width:"13rem"});
          this.columns.push({title: group.name + ' (p)', field: group.name + '(p)',
            mutator: this.pValueMutator(group.name), headerSort: true,
            headerSortStartingDir:'desc', width:"13rem"});
        }
      }
      // this.filters = []
    });
    this.geneSets$ = this.userData.geneSets$;
    this.geneSetNames$ = combineLatest([this.geneSets$, this.userData.platform$]).pipe(
      map(([geneSets, platform]) => {
        if (platform == undefined) {
          return [];
        } else {
          return Array.from(geneSets.values())
            .filter(g => g.platform === platform)
            .map(g => g.name)
            .sort();
        }
      })
    );
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
    this.ngZone.runOutsideAngular(() => {
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
              sorter: params.sorters[0],
              // filtering: this.filters,
              probes: this.probes
            }
            return(JSON.stringify(requestBodyObject));
          },
        },
        ajaxURLGenerator: function(url, _config, params: {page: number}){
          const page = params.page;
          return `${url}?offset=${((page - 1) * 100)}`;
        },
        ajaxFiltering: true,
        paginationDataReceived: {
          "data": "rows",
        },
        dataLoaded: (sampleTableComponent => {
          return function(this: Tabulator, data: Record<string, string>[]) {
            sampleTableComponent.tableData$.next(data);
            sampleTableComponent.dataFetched = true;
            sampleTableComponent.lastPage = (this.getPageMax() as number);
            sampleTableComponent.changeDetector.detectChanges();
            sampleTableComponent.tablePageNumber = (this.getPage() as number);
          };
        })(this),
        columns: this.columns,
        index: "probe",
        layout:"fitDataFill",
        height: "calc(100vh - 11.725rem)",
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
    });
  }

  onCreateGeneSet(name: string): void {
    const platform = this.enabledSampleGroups[0].platform;
    const data: {probe: string}[] | undefined = this.tabulator?.getData();
    const probes = data?.map(row => row.probe);

    if (probes == undefined) throw new Error("No probes available for gene set");

    const geneSet = {
      name: name,
      platform: platform,
      probes: probes
    } as GeneSet;

    this.userData.geneSets$.value.set(name, geneSet);
    this.userData.geneSets$.next(this.userData.geneSets$.value);

    this.onSelectGeneSet(name);
  }

  onSelectGeneSet(name: string): void {
    if (this.currentGeneSet != name) {
      const geneSet = this.userData.geneSets$.value.get(name);
      const probes = geneSet?.probes;
      if (probes) {
        this.probes = probes;
        this.tabulator?.setData(undefined);
        this.currentGeneSet = name;
      }
    }
  }

  onShowAllGenes(): void {
    if (this.probes.length > 0) {
      this.probes = [];
      this.tabulator?.setData(undefined);
      this.currentGeneSet = undefined;
    }
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
