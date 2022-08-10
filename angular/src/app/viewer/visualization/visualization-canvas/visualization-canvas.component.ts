import { Component, OnInit } from '@angular/core';
import cytoscape from 'cytoscape';

import { Network } from './network';

@Component({
  selector: 'app-visualization-canvas',
  templateUrl: './visualization-canvas.component.html',
  styleUrls: ['./visualization-canvas.component.scss']
})
export class VisualizationCanvasComponent implements OnInit {
	
	private _cy: any;
	private _net: Network;

  constructor() {	
		this._net = new Network();
	}

  ngOnInit(): void {
		this._cy = cytoscape({ 
			container: document.getElementById('cy'),
			style: [
				{
					selector: 'node',
					style: {
						'label': 'data(label)',
						'text-valign': 'center',
						'text-halign': 'center',
						'background-color': 'data(color)',
						'border-color': 'data(borderColor)',
						'border-width': '1px',
						'display': 'element',
					}
				},
				{
					selector: "edge",
					style :{
						"line-color": "data(color)",
					}
				}
			]
		});
		this._cy.add(this._net.getNodes());
		this._cy.add(this._net.getInteractions());

		let layout = this._cy.layout({name: 'concentric'});
		layout.run();
		this._cy.fit();

		
	}
}
