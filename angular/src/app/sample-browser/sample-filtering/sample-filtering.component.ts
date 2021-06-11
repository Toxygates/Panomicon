import { Component, EventEmitter, Input, Output } from '@angular/core';
import { SampleFilter, SampleFilterType } from '../../models/sample-filter.model';

@Component({
  selector: 'app-sample-filtering',
  templateUrl: './sample-filtering.component.html',
  styleUrls: ['./sample-filtering.component.scss']
})
export class SampleFilteringComponent {

  @Input() attributes: Set<string>;
  @Input() filters: SampleFilter[];
  @Output() submitFilters = new EventEmitter();

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
    if (this.filters.every(f => f.validate())) {
      this.submitFilters.emit();
    } else {
      this.haveTriedToSubmit = true;
    }
  }
}
