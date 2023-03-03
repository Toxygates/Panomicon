import { Component, ViewChild } from '@angular/core';
import { BehaviorSubject, combineLatest, Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import {
  GeneSet,
  SampleGroup,
} from 'src/app/shared/models/frontend-types.model';
import { UserDataService } from 'src/app/shared/services/user-data.service';
import { DisplayCanvasComponent } from '../display-canvas/display-canvas.component';
import { Network } from 'src/app/shared/models/backend-types.model';
import { BackendService } from 'src/app/shared/services/backend.service';

@Component({
  selector: 'app-network-display',
  templateUrl: './network-display.component.html',
  styleUrls: ['./network-display.component.scss'],
})
export class NetworkDisplayComponent {
  @ViewChild('networkCanvas')
  networkCanvas!: DisplayCanvasComponent;

  firstNetworkSampleGroupOptions$: Observable<SampleGroup[]>;
  secondNetworkSampleGroupOptions$: Observable<SampleGroup[]>;
  geneSets$: Observable<GeneSet[]>;

  firstNetworkSampleGroup$!: BehaviorSubject<string | null>;
  secondNetworkSampleGroup$!: BehaviorSubject<string | null>;
  networkGeneSet$: BehaviorSubject<string | null>;

  isReadyToGenerateNetwork$!: Observable<boolean>;

  fetchedNetwork$: BehaviorSubject<Network | null> =
    new BehaviorSubject<Network | null>(null);
  get fetchedNetwork(): Network | null {
    return this.fetchedNetwork$.value;
  }
  fetchingNetwork = false;

  constructor(
    private userData: UserDataService,
    private backend: BackendService
  ) {
    this.firstNetworkSampleGroupOptions$ = this.userData.sampleGroups$.pipe(
      map((sampleGroups) => {
        return [...sampleGroups.values()].filter(
          (group) => group.type === 'mRNA'
        );
      })
    );
    this.secondNetworkSampleGroupOptions$ = this.userData.sampleGroups$.pipe(
      map((sampleGroups) => {
        return [...sampleGroups.values()].filter(
          (group) => group.type === 'miRNA'
        );
      })
    );

    this.geneSets$ = userData.geneSets$.pipe(
      map((geneSets) => (geneSets ? [...geneSets.values()] : []))
    );

    this.firstNetworkSampleGroup$ = this.userData.firstNetworkSampleGroupName$;
    this.secondNetworkSampleGroup$ =
      this.userData.secondNetworkSampleGroupName$;
    this.networkGeneSet$ = this.userData.networkGeneSetName$;

    this.isReadyToGenerateNetwork$ = combineLatest([
      this.firstNetworkSampleGroup$,
      this.secondNetworkSampleGroup$,
      this.networkGeneSet$,
    ]).pipe(
      map(
        ([firstGroup, secondGroup, geneSet]) =>
          !!firstGroup && !!secondGroup && !!geneSet
      )
    );
  }

  generateNetwork(): void {
    this.fetchingNetwork = true;

    const currentNetworkGeneSet =
      this.networkGeneSet$.value !== null ? this.networkGeneSet$.value : '';
    const geneSet = this.userData.geneSets$.value.get(
      currentNetworkGeneSet //this.networkGeneSet$.value
    );

    const currentFirstNetworkSampleGroup =
      this.firstNetworkSampleGroup$.value !== null
        ? this.firstNetworkSampleGroup$.value
        : '';
    const sampleGroup1 = this.userData.sampleGroups$.value.get(
      currentFirstNetworkSampleGroup //this.firstNetworkSampleGroup$.value
    );

    const currentSecondNetworkSampleGroup =
      this.secondNetworkSampleGroup$.value !== null
        ? this.secondNetworkSampleGroup$.value
        : '';
    const sampleGroup2 = this.userData.sampleGroups$.value.get(
      currentSecondNetworkSampleGroup //this.secondNetworkSampleGroup$.value
    );

    let network$: Observable<Network>;
    if (
      sampleGroup1 !== undefined &&
      sampleGroup2 !== undefined &&
      geneSet !== undefined
    ) {
      if (geneSet?.type === sampleGroup1?.type) {
        network$ = this.backend.getNetwork(sampleGroup1, sampleGroup2, geneSet);
      } else {
        network$ = this.backend.getNetwork(sampleGroup2, sampleGroup1, geneSet);
      }
    } else {
      network$ = new Observable<Network>();
    }

    network$.subscribe(this.fetchedNetwork$);
    network$.subscribe(() => {
      this.fetchingNetwork = false;
    });
  }
}
