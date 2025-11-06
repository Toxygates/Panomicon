import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, TemplateRef } from '@angular/core';
import { BsModalRef, BsModalService } from 'ngx-bootstrap/modal';
import { BehaviorSubject, Observable } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { Platform } from 'src/app/shared/models/backend-types.model';
import { BackendService } from 'src/app/shared/services/backend.service';
import { FetchedDataService } from 'src/app/shared/services/fetched-data.service';
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
    private fetchedData: FetchedDataService,
    private modalService: BsModalService
  ) {
    this.targetMineUsername$ = this.userData.targetMineUsername$;
    this.targetMinePassword$ = this.userData.targetMinePassword$;
    this.platforms$ = this.fetchedData.platforms$;
  }

  geneSets$!: Observable<Map<string, GeneSet>>;
  platforms$!: BehaviorSubject<Platform[] | null>;

  modalRef: BsModalRef | undefined;
  targetMineUsername$: BehaviorSubject<string>;
  targetMinePassword$: BehaviorSubject<string>;
  replaceGeneSets = true;
  waitingForApiResponse = false;

  selectedPlatform = '';

  ngOnInit(): void {
    this.geneSets$ = this.userData.geneSets$;
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
        this.selectedPlatform
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
        const geneSets = this.userData.geneSets$.value;
        for (const fetchedGeneSet of fetchedGeneSets) {
          const platforms = this.fetchedData.platforms$.value;
          const platform = platforms?.find(
            (p) => p.id === this.selectedPlatform
          );
          const type = platform?.type;
          if (type === undefined) continue;

          const newGeneSet = {
            name: fetchedGeneSet.name,
            platform: this.selectedPlatform,
            probes: fetchedGeneSet.items,
            type,
          };
          if (this.replaceGeneSets || !geneSets.has(fetchedGeneSet.name)) {
            geneSets.set(fetchedGeneSet.name, newGeneSet);
          }
        }
        this.userData.geneSets$.next(geneSets);
        alert(`Gene sets imported`);
        this.modalRef?.hide();
      });
  }
}
