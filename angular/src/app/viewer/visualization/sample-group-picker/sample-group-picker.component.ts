import { Component, Input, Output, EventEmitter } from '@angular/core';
import { SampleGroup } from 'src/app/shared/models/frontend-types.model';

@Component({
  selector: 'app-sample-group-picker',
  templateUrl: './sample-group-picker.component.html',
  styleUrls: ['./sample-group-picker.component.scss'],
})
export class SampleGroupPickerComponent {
  selectedSampleGroup: string | null = null;
  @Output() selectedSampleGroupChange = new EventEmitter<string>();

  @Input() sampleGroupType!: string;
  @Input() sampleGroups!: SampleGroup[] | null;
  @Input() disabled!: boolean;

  selectSampleGroup(sampleGroup: string): void {
    this.selectedSampleGroup = sampleGroup;
    this.selectedSampleGroupChange.emit(sampleGroup);
  }
}
