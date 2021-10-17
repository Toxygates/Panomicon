import { Component, ElementRef, EventEmitter, HostListener, Input, OnInit, Output, ViewChild } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { Attribute } from '../../../shared/models/backend-types.model';
import { SampleFilter, SampleFilterType } from '../../../shared/models/sample-filter.model';

@Component({
  selector: 'app-sample-filtering',
  templateUrl: './sample-filtering.component.html',
  styleUrls: ['./sample-filtering.component.scss']
})
export class SampleFilteringComponent implements OnInit {

  constructor(private toastr: ToastrService) {}

  @Input() attributes!:  Set<string> | null;
  @Input() attributeMap!: Map<string, Attribute> | null;
  @Input() filters!: SampleFilter[];
  @Output() submitFilters = new EventEmitter<SampleFilter[]>();
  @Output() cancelEditFilters = new EventEmitter();

  @ViewChild('theWholeModal') modalDiv!: ElementRef<HTMLDivElement>;
  @ViewChild('theForm') formElement!: ElementRef<HTMLFormElement>;

  sampleFilterTypes = Object.values(SampleFilterType);

  dirty = false;
  documentClickEnabled = false;
  haveTriedToSubmit = false;

  ngOnInit(): void {
    this.filters = this.filters.map(f => f.clone());

    // Need to set this flag with a delay in order to ignore the
    // user button click that opened this modal.
    setTimeout(() => this.documentClickEnabled = true, 0);
  }

  appendNewFilter(): void {
    this.haveTriedToSubmit = false;
    this.filters.push(new SampleFilter());
    this.dirty = true;
  }

  removeFilter(filter: SampleFilter): void {
    this.filters.splice(this.filters.indexOf(filter), 1);
    this.dirty = true;
  }

  applyFilters(): void {
    if (this.attributeMap) {
      if (this.filters.every(f => f.validate(this.attributeMap as Map<string, Attribute>))) {
        this.submitFilters.emit(this.filters);
      } else {
        this.haveTriedToSubmit = true;
      }
    }
  }

  cancel(): void {
    this.cancelEditFilters.emit();
    this.toastr.clear();
  }

  possiblyCloseModal(): void {
    if (this.dirty ||
        this.formElement.nativeElement.classList.contains("ng-dirty")) {
      this.toastr.error("Press 'Cancel' to discard changes",
        "Unsaved filter changes");
    } else {
      this.cancel();
    }
  }

  @HostListener('window:keydown.esc', ['$event'])
  onEsc(event: KeyboardEvent): void {
    if (event.key == "Escape") {
      event.preventDefault();
      this.possiblyCloseModal();
    }
  }

  @HostListener('document:click', ['$event'])
  onClick(event: Event): void {
    if (this.documentClickEnabled) {
      if (!this.modalDiv.nativeElement.contains((event.target as Element))) {
        this.possiblyCloseModal();
      }
    }
  }

}
