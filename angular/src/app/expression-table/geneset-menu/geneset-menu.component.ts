import { Component, EventEmitter, Input, Output, TemplateRef, ViewChild } from '@angular/core';
import { BsModalRef, BsModalService } from 'ngx-bootstrap/modal';

@Component({
  selector: 'app-geneset-menu',
  templateUrl: './geneset-menu.component.html',
  styleUrls: ['./geneset-menu.component.scss']
})
export class GenesetMenuComponent {

  constructor(private modalService: BsModalService) {}

  modalRef: BsModalRef | undefined;
  @ViewChild('nameGeneSetModal') nameGeneSetTemplate!: TemplateRef<unknown>;

  @Input() geneSetNames!: string[];
  @Input() currentGeneSet: string | undefined;
  @Output() createGeneSet = new EventEmitter<string>();
  @Output() selectGeneSet = new EventEmitter<string>();
  @Output() showAllGenes = new EventEmitter();

  newGeneSetName: string | undefined;

  openGeneSetNameModal() : void {
    this.modalRef = this.modalService.show(this.nameGeneSetTemplate,
      { class: 'modal-dialog-centered' });
    this.modalRef.onHidden.subscribe(() => {
      this.newGeneSetName = undefined;
    })
  }

  submitModal() : void {
    this.modalRef?.hide();
    this.createGeneSet.emit(this.newGeneSetName);
  }
}
