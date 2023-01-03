import { AfterViewInit, Component, OnInit, ViewChild } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { GeneSet, SampleGroup } from 'src/app/shared/models/frontend-types.model';
import { UserDataService } from 'src/app/shared/services/user-data.service';
import { DisplayCanvasComponent } from '../display-canvas/display-canvas.component';
import { LayoutPickerComponent } from '../layout-picker/layout-picker.component';
import { Network } from './network';

@Component({
  selector: 'app-network-display',
  templateUrl: './network-display.component.html',
  styleUrls: ['./network-display.component.scss']
})
export class NetworkDisplayComponent implements AfterViewInit {

	public selectedLayout: any = {value: 'concentric', id: 'Concentric'};	
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
	
	constructor(
		private userData: UserDataService,
	) {
		this.network = new Network();
		this.firstNetworkSampleGroupOptions$ = this.userData.sampleGroups$.pipe(
			map(sampleGroups => {
				return [...sampleGroups.values()]
					.filter(group => group.type === 'mRNA');
			})
		);
		this.secondNetworkSampleGroupOptions$ = this.userData.sampleGroups$.pipe(
			map(sampleGroups => {
				return [...sampleGroups.values()]
					.filter(group => group.type === 'miRNA');
			})
		);

		this.geneSets$ = userData.geneSets$.pipe(
			map(geneSets => geneSets ? [...geneSets.values()] : [])
		);

		this.firstNetworkSampleGroup$ = userData.firstNetworkSampleGroupName$;
		this.secondNetworkSampleGroup$ = userData.secondNetworkSampleGroupName$;
		this.networkGeneSet$ = userData.networkGeneSetName$;
	}
	
	ngAfterViewInit(): void {
		console.log('Values at NetDisplayComp on ngAfterViewinit():');
		console.log('layoutPicker: ', this.layoutPicker);
	}

  // ngOnInit(): void {

  // }

	setSelectedLayout(layout: any): void{
		this.selectedLayout = layout;
		this.networkCanvas.updateLayout(this.selectedLayout.value);
	}

}
