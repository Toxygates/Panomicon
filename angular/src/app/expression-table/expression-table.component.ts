import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { BackendService } from '../backend.service';

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
  matrix: any;

  ngOnInit(): void {
    this.activatedRoute.queryParams.pipe(
        filter(params => params.samples)
      ).subscribe(params => {
        this.samples = params.samples;
        this.backend.getMatrix(this.samples)
        .subscribe(
          result => {
            this.matrix = JSON.stringify(result);
          }
        )
    });
  }

}
