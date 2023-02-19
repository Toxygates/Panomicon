import { HttpErrorResponse } from '@angular/common/http';
import { Component, Input, OnInit, TemplateRef } from '@angular/core';
import { BsModalRef, BsModalService } from 'ngx-bootstrap/modal';
import { BehaviorSubject, Observable } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { BackendService } from 'src/app/shared/services/backend.service';
import { GeneSet } from '../../../shared/models/frontend-types.model';
import { UserDataService } from '../../../shared/services/user-data.service';

@Component({
  selector: 'app-gene-set-manager',
  templateUrl: './gene-set-manager.component.html',
  styleUrls: ['./gene-set-manager.component.scss'],
})
export class GeneSetManagerComponent implements OnInit {
  constructor(
    private userData: UserDataService,
    private backend: BackendService,
    private modalService: BsModalService
  ) {
    this.targetMineUsername$ = this.userData.targetMineUsername$;
    this.targetMinePassword$ = this.userData.targetMinePassword$;
  }

  @Input() geneSets$!: Observable<Map<string, GeneSet>>;
  platforms$!: Observable<Set<string>>;

  modalRef: BsModalRef | undefined;
  targetMineUsername$: BehaviorSubject<string>;
  targetMinePassword$: BehaviorSubject<string>;
  waitingForApiResponse = false;

  platform = 'Rat230_2';

  ngOnInit(): void {
    this.geneSets$ = this.userData.geneSets$;
    this.platforms$ = this.geneSets$.pipe(
      map((geneSetMap) => {
        const platforms = new Set<string>();
        geneSetMap.forEach((geneSet) => {
          platforms.add(geneSet.platform);
        });
        return platforms;
      })
    );
  }

  geneSetsForPlatform$(platform: string): Observable<GeneSet[]> {
    return this.geneSets$.pipe(
      map((geneSets) => {
        return Array.from(geneSets.values()).filter(
          (geneSet) => geneSet.platform == platform
        );
      })
    );
  }

  openGeneSetImportModal(template: TemplateRef<unknown>): void {
    this.modalRef = this.modalService.show(template, {
      class: 'modal-dialog-centered modal-lg',
      keyboard: false,
    });
  }

  importGeneSets(): void {
    this.waitingForApiResponse = true;
    this.backend
      .importGeneSets(
        this.targetMineUsername$.value,
        this.targetMinePassword$.value,
        'Rat230_2'
      )
      .pipe(
        catchError((error: HttpErrorResponse) => {
          this.waitingForApiResponse = false;
          alert(`Error importing gene sets: ${error.message}`);
          throw error;
        })
      )
      .subscribe((fetchedGeneSets) => {
        this.waitingForApiResponse = false;
        console.dir(fetchedGeneSets);
        const geneSets = this.userData.geneSets$.value;
        for (const fetchedGeneSet of fetchedGeneSets) {
          const newGeneSet = {
            name: fetchedGeneSet.name,
            platform: this.platform,
            probes: fetchedGeneSet.items,
          };
          geneSets.set(fetchedGeneSet.name, newGeneSet);
        }
        this.userData.geneSets$.next(geneSets);
        alert(`Gene sets imported`);
        this.modalRef?.hide();
      });
  }
}
