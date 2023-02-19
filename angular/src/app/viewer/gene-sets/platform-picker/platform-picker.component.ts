import { Component, Output, EventEmitter, Input } from '@angular/core';
import { Platform } from 'src/app/shared/models/backend-types.model';

@Component({
  selector: 'app-platform-picker',
  templateUrl: './platform-picker.component.html',
  styleUrls: ['./platform-picker.component.scss'],
})
export class PlatformPickerComponent {
  @Input() selectedPlatform!: string | null;
  @Output() selectedPlatformChange = new EventEmitter<string>();

  @Input() platforms!: Platform[] | null;

  selectPlatform(platformId: string): void {
    this.selectedPlatform = platformId;
    this.selectedPlatformChange.emit(platformId);
  }
}
