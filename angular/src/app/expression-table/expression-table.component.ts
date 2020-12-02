import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import Tabulator from 'tabulator-tables';

@Component({
  selector: 'app-expression-table',
  templateUrl: './expression-table.component.html',
  styleUrls: ['./expression-table.component.scss']
})
export class ExpressionTableComponent implements OnInit {

  constructor(private activatedRoute: ActivatedRoute,
    private router: Router) { }

  samples: string[] = [];
  data: any;

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

  ngOnInit(): void {
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
    new Tabulator("#my-tabular-table", {
      ajaxURL: "json/matrix",
      ajaxConfig:"POST",
      ajaxContentType: {
        headers: {
          'Content-Type': 'application/json',
        },
        body: function(_url, _config, _params) {
          var requestBodyObject = {
            "groups": [{ "name": "Group 1", "sampleIds": _this.samples }]
          };
          console.log(requestBodyObject);
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
      pagination:"remote",
      columns: this.columns,
      layout:"fitDataTable",
      maxHeight: "75vh",
    });
  }

}
