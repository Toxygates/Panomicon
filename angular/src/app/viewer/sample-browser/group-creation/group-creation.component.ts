import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-group-creation',
  templateUrl: './group-creation.component.html',
  styleUrls: ['./group-creation.component.scss'],
})
export class GroupCreationComponent {
  sampleGroupName: string | undefined;
  @Input() selectedTreatmentGroups = new Set<string>();
  @Input() collapsed = true;
  @Output() sampleGroupSaved = new EventEmitter<string>();

  saveSampleGroup(): void {
    this.sampleGroupSaved.emit(this.sampleGroupName);
    this.sampleGroupName = undefined;
  }
}
