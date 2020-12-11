import { AfterViewInit, ChangeDetectorRef, Component, HostListener, ViewChild } from '@angular/core';
import { BsModalService, BsModalRef } from 'ngx-bootstrap/modal';
import { ActivatedRoute, Router } from '@angular/router';
import Tabulator from 'tabulator-tables';

@Component({
  selector: 'app-expression-table',
  templateUrl: './expression-table.component.html',
  styleUrls: ['./expression-table.component.scss']
})
export class ExpressionTableComponent implements AfterViewInit {

  constructor(private activatedRoute: ActivatedRoute,
    private router: Router, private modalService: BsModalService,
    private changeDetector: ChangeDetectorRef) { }

  tabulator: Tabulator;
  modalRef: BsModalRef;
  @ViewChild('tabulatorContainer') tabulatorContainer;
  @ViewChild('modalTemplate') modalTemplate;

  samples: string[];
  dataFetched = false;
  lastPage = 0;
  tablePageNumber = 0;
  goToPageSubmitEnabled = false;

  geneSymbolsMutator(_value, data, _type, _params, _component): string {
    return data.probeTitles.join(" / ");
  }

  probeTitlesMutator(_value, data, _type, _params, _component): string {
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

  columns = [
    {title: 'Gene Symbol', field: 'type',
      mutator: this.geneSymbolsMutator, headerSort:false},
    {title: 'Probe Titles', field: 'organism',
      mutator: this.probeTitlesMutator, headerSort:false},
    {title: 'Probe', field: 'probe', headerSort:false},
    {title: 'Log2-fold', field: 'Group 1', mutator: this.log2foldMutator},
    {title: 'P-Value', field: 'Group 1(p)', mutator: this.pValueMutator},
  ]

  ngAfterViewInit(): void {
    this.activatedRoute.queryParamMap.subscribe(paramMap => {
      if (!paramMap.has("samples")) {
        this.router.navigate(['']);
      } else {
        this.samples = paramMap.getAll("samples");
        this.drawTable();
      }
    });
  }

  private drawTable(): void {
    var _this = this;
    var tabulatorElement = document.createElement('div');
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
          var requestBodyObject: any  = {
            "groups": [{ "name": "Group 1", "sampleIds": _this.samples }],
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
      layout:"fitDataTable",
      maxHeight: "75vh",
      columnHeaderSortMulti:false,
      ajaxSorting:true,
      initialSort:[
        {column:"Group 1", dir:"desc"}
      ],
      tooltips:true,
      ajaxLoaderLoading: "<div class=\"spinner-border text-secondary\" role=\"status\"><span class=\"sr-only\">Loading...</span></div>",
      footerElement:"<div style=\"float: left;\"><button class=\"tabulator-page\" style=\"border-radius: 4px; border: 1px solid #dee2e6;\" onclick=\"window.dispatchEvent(new CustomEvent(\'OpenModal\'));\">Go to page...</button></div>",
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
