import { Component } from '@angular/core';
import { FetchedDataService } from 'src/app/shared/services/fetched-data.service';
import { Attribute, Sample } from '../../../shared/models/backend-types.model'

interface OverviewRow {
  representative: Sample;
  count: number;
}

@Component({
  selector: 'app-batch-statistics',
  templateUrl: './batch-statistics.component.html',
  styleUrls: ['./batch-statistics.component.scss']
})
export class BatchStatisticsComponent {

  constructor(public fetchedData: FetchedDataService) { }

  samples$ = this.fetchedData.samples$;
  attributes$ = this.fetchedData.attributes$;

  selectedAttributes: Attribute[] = [];
  displayedAttributes: Attribute[] = [];

  entries: OverviewRow[] = [];
  entryMap = new Map<string, OverviewRow>();

  addAttribute(attribute: Attribute): void {
    this.selectedAttributes.push(attribute);
    if (!this.fetchedData.fetchedAttributes$.value.has(attribute.id)) {
      console.log("fetching", attribute);
      this.fetchedData.fetchAttribute(attribute);
    }
  }

  removeAttribute(index: number): void {
    this.selectedAttributes.splice(index, 1);
  }

  changeAttribute(index: number, attribute: Attribute): void {
    this.selectedAttributes[index] = attribute;
  }

  keyForSample(sample: Sample): string {
    return this.selectedAttributes.map(attribute =>
      [attribute.id, sample[attribute.id]]).join("-");
  }

  compare = (r1: OverviewRow, r2: OverviewRow): number => {
    for (const attribute of this.selectedAttributes) {
      const v1 = r1.representative[attribute.id];
      const v2 = r2.representative[attribute.id];
      if (!attribute.isNumerical) {
        const comparison = v1.localeCompare(v2);
        if (comparison !== 0) return comparison;
      } else {
        const n1 = parseFloat(v1);
        const n2 = parseFloat(v2);
        if (isNaN(n1) && !isNaN(n2)) return 1;
        if (!isNaN(n1) && isNaN(n2)) return 2;
        if (n1 > n2) return 1;
        if (n2 > n1) return -1;
      }
    }
    return 0;
  }

  generateEntries(): void {
    this.entries = [];
    this.entryMap.clear();
    if (this.samples$.value) {
      this.samples$.value?.forEach(sample => {
        const key = this.keyForSample(sample);
        if (this.entryMap.has(key)) {
          const entry = this.entryMap.get(key) as OverviewRow;
          entry.count += 1;
        } else {
          const newEntry = {representative: sample, count: 1};
          this.entryMap.set(key, newEntry);
          this.entries.push(newEntry);
        }
      });
      this.entries.sort(this.compare)
    }
    this.displayedAttributes = [...this.selectedAttributes];
    console.log(this.entries);
  }
}
