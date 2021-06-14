import { Component, EventEmitter, Input, Output } from '@angular/core';
import { IAttribute } from 'src/app/models/backend-types.model';
import { SampleFilter, SampleFilterType } from '../../models/sample-filter.model';

@Component({
  selector: 'app-sample-filtering',
  templateUrl: './sample-filtering.component.html',
  styleUrls: ['./sample-filtering.component.scss']
})
export class SampleFilteringComponent {

  @Input() attributes!:  Set<IAttribute>;
  @Input() attributeMap!: Map<string, IAttribute>;
  @Input() filters!: SampleFilter[];
  @Output() submitFilters = new EventEmitter<SampleFilter[]>();

  sampleFilterTypes: string[] = Object.values(SampleFilterType);

  haveTriedToSubmit = false;

  appendNewFilter(): void {
    this.haveTriedToSubmit = false;
    this.filters.push(new SampleFilter());
  }

  removeFilter(filter: SampleFilter): void {
    this.filters.splice(this.filters.indexOf(filter), 1);
  }

  applyFilters(): void {
    if (this.filters.every(f => f.validate(this.attributeMap))) {
      this.submitFilters.emit(this.filters);
    } else {
      this.haveTriedToSubmit = true;
    }
  }
}
