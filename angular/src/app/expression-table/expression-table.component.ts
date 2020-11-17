import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { BackendService } from '../backend.service';
import Tabulator from 'tabulator-tables';

import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-expression-table',
  templateUrl: './expression-table.component.html',
  styleUrls: ['./expression-table.component.scss']
})
export class ExpressionTableComponent implements OnInit {

  constructor(private activatedRoute: ActivatedRoute,
    private backend: BackendService, private router: Router) { }

  samples: string[] = [];
  data: any;

  geneSymbolsMutator(_value, data, _type, _params, _component): string {
    return data.probeTitles.join(" / ");
  }

  probeTitlesMutator(_value, data, _type, _params, _component): string {
    return data.geneSymbols.join(" / ");
  }

  log2foldMutator(_value, data, _type, _params, _component): string {
    var value = data.values[0];
    if (typeof(value) != "number") {
      console.log("Non-numerical value: in log2fold column: " + value);
      return "NaN";
    }
    return value.toFixed(3);
  }

  pValueMutator(_value, data, _type, _params, _component): string {
    var value = data.values[1];
    if (typeof(value) != "number") {
      console.log("Non-numerical value: in p-value column: " + value);
      return "NaN";
    }
    if (value > .00005) {
      return value.toFixed(4);
    } else {
      return value.toExponential(3);
    }
  }

  columns = [
    {title: 'Gene Symbol', field: 'type', mutator: this.geneSymbolsMutator},
    {title: 'Probe Titles', field: 'organism', mutator: this.probeTitlesMutator},
    {title: 'Probe', field: 'probe'},
    {title: 'Log2-fold', field: 'log2fold', mutator: this.log2foldMutator},
    {title: 'P-Value', field: 'pValue', mutator: this.pValueMutator},
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
        body:function(_url, _config, _params) {
          var requestBodyObject = {
            "groups": [{ "name": "Group 1", "sampleIds": _this.samples }]
          };
          console.log(requestBodyObject);
          return(JSON.stringify(requestBodyObject));
        },
      },
      ajaxResponse: function(_url, _params, response) {
        return response.rows;
      },
      columns: this.columns,
      layout:"fitDataTable",
      maxHeight: "75vh",
    });
  }

}
