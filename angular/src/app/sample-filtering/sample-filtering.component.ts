import { Component, Input, OnInit } from '@angular/core';
import { SampleFilter, SampleFilterType } from '../models/sample-filter.model';

@Component({
  selector: 'app-sample-filtering',
  templateUrl: './sample-filtering.component.html',
  styleUrls: ['./sample-filtering.component.scss']
})
export class SampleFilteringComponent implements OnInit {

  @Input() filters: SampleFilter[];

  sampleFilterTypes: string[] = Object.values(SampleFilterType);

  @Input() attributes: Set<string>;

  ngOnInit(): void {}

  assignFilterType(filter: SampleFilter, typeString: string) {
    filter.type = typeString as SampleFilterType;
  }

  appendNewFilter() {
    this.filters.push(new SampleFilter());
  }

  removeFilter(filter: SampleFilter) {
    this.filters = this.filters.filter(item => item !== filter);
  }
}
