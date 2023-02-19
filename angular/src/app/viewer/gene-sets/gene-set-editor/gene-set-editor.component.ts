import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, TemplateRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { BsModalRef, BsModalService } from 'ngx-bootstrap/modal';
import { ToastrService } from 'ngx-toastr';
import { BehaviorSubject, combineLatest, Observable } from 'rxjs';
import { catchError, first, map } from 'rxjs/operators';
import { BackendService } from 'src/app/shared/services/backend.service';
import { GeneSet } from '../../../shared/models/frontend-types.model';
import { UserDataService } from '../../../shared/services/user-data.service';

@Component({
  selector: 'app-gene-set-editor',
  templateUrl: './gene-set-editor.component.html',
  styleUrls: ['./gene-set-editor.component.scss'],
})
export class GeneSetEditorComponent implements OnInit {
  constructor(
    private userData: UserDataService,
    private toastr: ToastrService,
    private route: ActivatedRoute,
    private router: Router,
    private backend: BackendService,
    private modalService: BsModalService
  ) {
    this.targetMineUsername$ = this.userData.targetMineUsername$;
    this.targetMinePassword$ = this.userData.targetMinePassword$;
  }

  geneSetName$!: Observable<string | undefined>;
  geneSet$!: Observable<GeneSet | undefined>;

  newProbesText = '';

  modalRef: BsModalRef | undefined;
  targetMineUsername$: BehaviorSubject<string>;
  targetMinePassword$: BehaviorSubject<string>;
  waitingForApiResponse = false;

  ngOnInit(): void {
    this.geneSetName$ = this.route.paramMap.pipe(
      map((params) => params.get('geneSetName') as string | undefined)
    );

    this.geneSet$ = combineLatest([
      this.userData.geneSets$,
      this.geneSetName$,
    ]).pipe(
      map(([geneSets, geneSetName]) => {
        return geneSetName ? geneSets.get(geneSetName) : undefined;
      })
    );
  }

  deleteProbes(geneSet: GeneSet, probes: string[]): void {
    geneSet.probes = geneSet.probes.filter((probe) => !probes.includes(probe));
    this.userData.geneSets$.value.set(geneSet.name, geneSet);
    this.userData.geneSets$.next(this.userData.geneSets$.value);
    this.toastr.success(
      `Deleted ${probes.join(', ')}`,
      `Deleted ${probes.length} probes`
    );
  }

  addProbes(geneSet: GeneSet, text: string): void {
    const candidates = [...new Set(text.split(/[\s,]+/))];
    const existing = new Set(geneSet.probes);
    const toAdd = candidates.filter((p) => p.length > 0 && !existing.has(p));
    if (toAdd.length > 0) {
      geneSet.probes = geneSet.probes.concat(toAdd);
      this.toastr.success(
        `Added ${toAdd.join(', ')}`,
        `Added ${toAdd.length} probes`
      );
      this.userData.geneSets$.value.set(geneSet.name, geneSet);
      this.userData.geneSets$.next(this.userData.geneSets$.value);
      this.newProbesText = '';
    } else {
      this.toastr.error('No new probes to add', 'Error');
    }
  }

  deleteGeneSet(name: string): void {
    this.userData.geneSets$.value.delete(name);
    this.userData.geneSets$.next(this.userData.geneSets$.value);
    void this.router.navigate(['..'], { relativeTo: this.route });
  }

  openGeneSetExportModal(template: TemplateRef<unknown>): void {
    this.modalRef = this.modalService.show(template, {
      class: 'modal-dialog-centered modal-lg',
      keyboard: false,
    });
  }

  exportGeneSet(): void {
    this.waitingForApiResponse = true;
    this.geneSet$.pipe(first()).subscribe((geneSet) => {
      if (!geneSet) {
        // The export button is only displayed if a valid gene set is selected,
        // so this code path should be unreachable
        return;
      }
      this.backend
        .exportGeneSet(
          this.targetMineUsername$.value,
          this.targetMinePassword$.value,
          geneSet
        )
        .pipe(
          catchError((error: HttpErrorResponse) => {
            this.waitingForApiResponse = false;
            alert(`Error exporting gene set: ${error.message}`);
            throw error;
          })
        )
        .subscribe((_result) => {
          this.waitingForApiResponse = false;
          alert(`Gene set ${geneSet?.name || ''} exported`);
          this.modalRef?.hide();
        });
    });
  }
}
