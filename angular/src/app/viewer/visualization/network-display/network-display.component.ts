import { AfterViewInit, Component, OnInit, ViewChild } from '@angular/core';
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
	
	constructor() { 
		this.network = new Network();
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
