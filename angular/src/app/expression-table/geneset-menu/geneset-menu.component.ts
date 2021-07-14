import { Component, EventEmitter, Input, OnDestroy, Output, TemplateRef, ViewChild } from '@angular/core';
import { BsModalRef, BsModalService } from 'ngx-bootstrap/modal';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-geneset-menu',
  templateUrl: './geneset-menu.component.html',
  styleUrls: ['./geneset-menu.component.scss']
})
export class GenesetMenuComponent implements OnDestroy {

  constructor(private modalService: BsModalService) {}

  modalRef: BsModalRef | undefined;
  @ViewChild('nameGeneSetModal') nameGeneSetTemplate!: TemplateRef<unknown>;
  modalCloseSubscription: Subscription | undefined;

  @Input() geneSetNames!: string[];
  @Input() currentGeneSet: string | undefined;
  @Output() createGeneSet = new EventEmitter<string>();
  @Output() selectGeneSet = new EventEmitter<string>();
  @Output() showAllGenes = new EventEmitter();

  newGeneSetName: string | undefined;

  ngOnDestroy(): void {
    this.modalCloseSubscription?.unsubscribe();
  }

  openGeneSetNameModal(): void {
    this.modalCloseSubscription?.unsubscribe();
    this.modalRef = this.modalService.show(this.nameGeneSetTemplate,
      { class: 'modal-dialog-centered' });
    this.modalCloseSubscription = this.modalRef.onHidden.subscribe(() => {
      this.newGeneSetName = undefined;
    })
  }

  submitModal(): void {
    this.modalRef?.hide();
    this.createGeneSet.emit(this.newGeneSetName);
  }
}
