import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
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
    private backend: BackendService) { }

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
    this.activatedRoute.queryParamMap.pipe(
        filter(paramMap => paramMap.has("samples"))
      ).subscribe(params => {
        this.samples = params.getAll("samples");
        this.backend.getMatrix(this.samples)
        .subscribe(
          result => {
            console.log(JSON.stringify(result));
            this.data = result["rows"];
            this.drawTable();
          }
        )
    });
  }

  private drawTable(): void {
    var _this = this;
    new Tabulator("#my-tabular-table", {
      data: this.data,
      columns: this.columns,
      layout:"fitDataTable",
      maxHeight: "75vh",
    });
  }

}
