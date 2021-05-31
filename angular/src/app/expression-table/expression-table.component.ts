import { AfterViewInit, OnInit, ChangeDetectorRef, Component, HostListener, ViewChild } from '@angular/core';
import { BsModalService, BsModalRef } from 'ngx-bootstrap/modal';
import { ActivatedRoute, Router } from '@angular/router';
import { UserDataService } from '../user-data.service';
import { ISampleGroup } from '../models/sample-group.model'
import Tabulator from 'tabulator-tables';

@Component({
  selector: 'app-expression-table',
  templateUrl: './expression-table.component.html',
  styleUrls: ['./expression-table.component.scss']
})
export class ExpressionTableComponent implements OnInit, AfterViewInit {

  constructor(private userData: UserDataService,
    private activatedRoute: ActivatedRoute,
    private router: Router, private modalService: BsModalService,
    private changeDetector: ChangeDetectorRef) { }

  tabulator: Tabulator;
  modalRef: BsModalRef;
  @ViewChild('tabulatorContainer') tabulatorContainer;
  @ViewChild('modalTemplate') modalTemplate;

  enabledSampleGroups: ISampleGroup[];
  dataFetched = false;
  lastPage = 0;
  tablePageNumber = 0;
  goToPageSubmitEnabled = false;

  probeTitlesMutator(_value, data, _type, _params, _component): string {
    return data.probeTitles.join(" / ");
  }

  geneSymbolsMutator(_value, data, _type, _params, _component): string {
    return data.geneSymbols.join(" / ");
  }

  log2foldMutator(value, _data, _type, _params, _component): string {
    return Number(value).toFixed(3);
  }

  pValueMutator(value, data, _type, _params, _component): string {
    var numericalValue = Number(value)
    if (numericalValue > .00005) {
      return numericalValue.toFixed(4);
    } else {
      return numericalValue.toExponential(3);
    }
  }

  columns: any[] = [
    {title: 'Gene symbols', field: 'geneSymbols',
      mutator: this.geneSymbolsMutator, headerSort:false,
      width:"15rem"},
    {title: 'Probe titles', field: 'probeTitles',
      mutator: this.probeTitlesMutator, headerSort:false,
      width:"50rem"},
    {title: 'Probe', field: 'probe', headerSort:false},
  ]

  ngOnInit(): void {
    this.userData.enabledGroupsBehaviorSubject.subscribe(enabledGroups => {
      this.enabledSampleGroups = enabledGroups;
      if (enabledGroups.length == 0) {
        this.router.navigate(['']);
      } else {
        for (let group of enabledGroups) {
          this.columns.push({title: group.name, field: group.name,
            headerSort: true, mutator: this.log2foldMutator,
            headerSortStartingDir:"desc", width:"15rem"});
          this.columns.push({title: group.name + ' (p)', field: group.name + '(p)',
            headerSort: true, mutator: this.log2foldMutator,
            headerSortStartingDir:"desc", width:"15rem"});
        }
      }
    });
  }

  ngAfterViewInit(): void {
    this.drawTable();
  }

  private drawTable(): void {
    let _this = this;
    let tabulatorElement = document.createElement('div');
    tabulatorElement.style.width = "auto";
    this.tabulatorContainer.nativeElement.appendChild(tabulatorElement);
    this.tabulator = new Tabulator(tabulatorElement, {
      pagination:"remote",
      ajaxURL: "json/matrix",
      ajaxConfig:"POST",
      ajaxContentType: {
        headers: {
          'Content-Type': 'application/json',
        },
        body: function(_url, _config, params) {
          let groupInfoArray = [];
          for (let group of _this.enabledSampleGroups) {
            groupInfoArray.push({ "name": group.name, "sampleIds": group.samples })
          }
          let requestBodyObject: any  = {
            "groups": groupInfoArray,
          }
          requestBodyObject.page = params.page;
          requestBodyObject.sorter = params.sorters[0];
          return(JSON.stringify(requestBodyObject));
        },
      },
      ajaxURLGenerator: function(url, _config, params){
        var page = params.page;
        return url + "?offset=" + ((page - 1) * 100);
      },
      paginationDataReceived: {
        "data": "rows",
      },
      dataLoaded:function(_data){
        _this.dataFetched = true;
        _this.lastPage = this.getPageMax();
        _this.changeDetector.detectChanges();
        _this.tablePageNumber = this.getPage();
      },
      columns: this.columns,
      index: "probe",
      layout:"fitDataTable",
      height: "calc(100vh - 8.375rem)",
      columnHeaderSortMulti:false,
      ajaxSorting:true,
      initialSort:[
        {column:_this.enabledSampleGroups[0].name, dir:"desc"}
      ],
      tooltips:true,
      tooltipsHeader:true,
      ajaxLoaderLoading: "<div class=\"spinner-border text-secondary\" role=\"status\"><span class=\"sr-only\">Loading...</span></div>",
      footerElement:"<div class=\"d-none d-lg-block\" style=\"float: left;\"><button class=\"tabulator-page\" style=\"border-radius: 4px; border: 1px solid #dee2e6;\" onclick=\"window.dispatchEvent(new CustomEvent(\'OpenModal\'));\">Go to page...</button></div>",
    });
  }

  @HostListener("window:OpenModal")
  onOpenModal() {
    this.modalRef = this.modalService.show(this.modalTemplate,
      Object.assign({}, { class: 'modal-dialog-centered' }));
  }

  onSubmitModal() {
    this.tabulator.setPage(this.tablePageNumber);
    this.modalRef.hide();
  }
}
