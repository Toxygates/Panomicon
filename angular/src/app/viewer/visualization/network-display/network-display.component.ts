import { AfterViewInit, Component, ViewChild } from '@angular/core';
import { BehaviorSubject, combineLatest, Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import {
  GeneSet,
  SampleGroup,
} from 'src/app/shared/models/frontend-types.model';
import { UserDataService } from 'src/app/shared/services/user-data.service';
import { DisplayCanvasComponent } from '../display-canvas/display-canvas.component';
import { LayoutPickerComponent } from '../layout-picker/layout-picker.component';
import { Network } from './network';
import { Network as BackendNetwork } from '../../../shared/models/backend-types.model';
import { BackendService } from 'src/app/shared/services/backend.service';

@Component({
  selector: 'app-network-display',
  templateUrl: './network-display.component.html',
  styleUrls: ['./network-display.component.scss'],
})
export class NetworkDisplayComponent implements AfterViewInit {
  public selectedLayout: any = { value: 'concentric', id: 'Concentric' };
  public network: Network;

  @ViewChild('layoutPicker')
  layoutPicker!: LayoutPickerComponent;

  @ViewChild('networkCanvas')
  networkCanvas!: DisplayCanvasComponent;

  firstNetworkSampleGroupOptions$!: Observable<SampleGroup[]>;
  secondNetworkSampleGroupOptions$!: Observable<SampleGroup[]>;
  geneSets$!: Observable<GeneSet[]>;

  firstNetworkSampleGroup$!: BehaviorSubject<string | null>;
  secondNetworkSampleGroup$!: BehaviorSubject<string | null>;
  networkGeneSet$!: BehaviorSubject<string | null>;

  isReadyToGenerateNetwork$!: Observable<boolean>;

  fetchedNetwork$: BehaviorSubject<BackendNetwork | null> =
    new BehaviorSubject<BackendNetwork | null>(null);
  fetchingNetwork = false;

  constructor(
    private userData: UserDataService,
    private backend: BackendService
  ) {
    this.network = new Network();
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

    this.firstNetworkSampleGroup$ = userData.firstNetworkSampleGroupName$;
    this.secondNetworkSampleGroup$ = userData.secondNetworkSampleGroupName$;
    this.networkGeneSet$ = userData.networkGeneSetName$;

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

    // TODO: remove this since it's just for debugging
    this.fetchedNetwork$.subscribe((network) =>
      console.log(JSON.stringify(network))
    );
  }

  ngAfterViewInit(): void {
    console.log('Values at NetDisplayComp on ngAfterViewinit():');
    console.log('layoutPicker: ', this.layoutPicker);
  }

  generateNetwork(): void {
    this.fetchingNetwork = true;

    // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
    const geneSet = this.userData.geneSets$.value.get(
      this.networkGeneSet$.value!
    );
    // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
    const sampleGroup1 = this.userData.sampleGroups$.value.get(
      this.firstNetworkSampleGroup$.value!
    );
    // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
    const sampleGroup2 = this.userData.sampleGroups$.value.get(
      this.secondNetworkSampleGroup$.value!
    );

    let network$: Observable<BackendNetwork>;
    if (geneSet?.type === sampleGroup1?.type) {
      // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
      network$ = this.backend.getNetwork(
        sampleGroup1!,
        sampleGroup2!,
        geneSet!
      );
    } else {
      // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
      network$ = this.backend.getNetwork(
        sampleGroup2!,
        sampleGroup1!,
        geneSet!
      );
    }

    network$.subscribe(this.fetchedNetwork$);
    network$.subscribe(() => {
      this.fetchingNetwork = false;
    });
  }

  // ngOnInit(): void {

  // }

  setSelectedLayout(layout: any): void {
    // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
    this.selectedLayout = layout;
    // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access
    this.networkCanvas.updateLayout(this.selectedLayout.value);
  }
}
