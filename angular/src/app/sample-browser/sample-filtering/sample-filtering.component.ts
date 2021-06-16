import { Component, EventEmitter, Input, OnInit, Output, TemplateRef, ViewChild } from '@angular/core';
import { BsModalService, BsModalRef } from 'ngx-bootstrap/modal';
import { Observable } from 'rxjs';
import { IAttribute } from 'src/app/models/backend-types.model';
import { SampleFilter, SampleFilterType } from '../../models/sample-filter.model';

@Component({
  selector: 'app-sample-filtering',
  templateUrl: './sample-filtering.component.html',
  styleUrls: ['./sample-filtering.component.scss']
})
export class SampleFilteringComponent implements OnInit {

  constructor(private modalService: BsModalService) {}

  modalRef: BsModalRef | undefined;
  @ViewChild('modal') template!: TemplateRef<unknown>;

  @Input() attributes!:  Set<IAttribute>;
  @Input() attributeMap!: Map<string, IAttribute>;
  @Input() filters!: SampleFilter[];
  @Input() openModalObservable!: Observable<void>;
  @Output() submitFilters = new EventEmitter<SampleFilter[]>();

  sampleFilterTypes = Object.values(SampleFilterType);

  haveTriedToSubmit = false;

  ngOnInit(): void {
    this.openModalObservable.subscribe(_next => {
      this.openModal();
    });
  }

  openModal(): void {
    this.modalRef = this.modalService.show(this.template,
      { class: 'modal-dialog-centered modal-lg',
        ignoreBackdropClick: true });
    this.filters = this.filters.map(f => f.clone());
  }

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
      this.modalRef?.hide();
    } else {
      this.haveTriedToSubmit = true;
    }
  }
}
