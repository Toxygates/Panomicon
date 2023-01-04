import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { GeneSet } from 'src/app/shared/models/frontend-types.model';

@Component({
  selector: 'app-gene-set-picker',
  templateUrl: './gene-set-picker.component.html',
  styleUrls: ['./gene-set-picker.component.scss']
})
export class GeneSetPickerComponent implements OnInit {

  selectedGeneSet: GeneSet | null = null;
  @Output() selectedGeneSetChange = new EventEmitter<string>();

  @Input() geneSets$!: Observable<GeneSet[]>;
  @Input() disabled!: boolean;

  mrnaGeneSets$!: Observable<GeneSet[]>;
  mirnaGeneSets$!: Observable<GeneSet[]>;

  ngOnInit(): void {
    this.mrnaGeneSets$ = this.geneSets$.pipe(
      map(geneSets => {
        return geneSets.filter(geneSet => geneSet.type === 'mRNA');
      })
    )
    this.mirnaGeneSets$ = this.geneSets$.pipe(
      map(geneSets => {
        return geneSets.filter(geneSet => geneSet.type === 'miRNA');
      })
    )
  }

  selectGeneSet(geneSet: GeneSet): void {
    this.selectedGeneSet = geneSet;
    this.selectedGeneSetChange.emit(geneSet.name);
  }
}
