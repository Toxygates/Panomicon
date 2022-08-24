import { Component, Input, OnInit } from '@angular/core';

import { Network } from './network';

@Component({
  selector: 'app-network-display',
  templateUrl: './network-display.component.html',
  styleUrls: ['./network-display.component.scss']
})
export class NetworkDisplayComponent implements OnInit {

	public selectedLayout: any = {value: 'concentric', id: 'Concentric'};

	private _cy: any;
	private _net: Network;
	

  constructor() { 
		this._net = new Network();
	}

  ngOnInit(): void {

  }



}
