import { Component, OnInit } from '@angular/core';
import { BackendService } from '../backend.service'

@Component({
  selector: 'app-datasets',
  templateUrl: './datasets.component.html',
  styleUrls: ['./datasets.component.css']
})
export class DatasetsComponent implements OnInit {

  constructor(private backend: BackendService) { }

  datasets: any;

  ngOnInit(): void {
    this.backend.getDatasets()
      .subscribe(
        result => {
          window["blarg"] = result
          this.datasets = result
        })
  }

}
