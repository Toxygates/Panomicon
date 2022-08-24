import { Component, Input, OnInit } from '@angular/core';
import { Network } from '../network-display/network';
import * as cytoscape from 'cytoscape';

@Component({
  selector: 'app-display-canvas',
  templateUrl: './display-canvas.component.html',
  styleUrls: ['./display-canvas.component.scss']
})
export class DisplayCanvasComponent implements OnInit {

	@Input() network: Network | undefined;

	private _cy: any;

  constructor() {
		//cytoscape('core', 'updateLayout', updateLayout);
	}

  ngOnInit(): void {
		console.log(this.network);
		if( this.network !== undefined ){
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
		
			this._cy.add(this.network.getNodes());
			this._cy.add(this.network.getInteractions());
		}
		
		let layout = this._cy.layout({name: 'concentric'});
		layout.run();
		this._cy.fit();
		console.log(this._cy.updateLayout());
  }

	public updateLayout(name=null, boundingBox=undefined){
		let layout = this._cy.layout({
			name: name,
			fit: true, // whether to fit to viewport
			padding: 0, // fit padding
			boundingBox: boundingBox, // constrain layout bounds; { x1, y1, x2, y2 } or { x1, y1, w, h }
			animate: false, // whether to transition the node positions
			animationDuration: 500, // duration of animation in ms if enabled
			animationEasing: undefined, // easing of animation if enabled
			// animateFilter: function ( node, i ){ return true; }, // a function that determines whether the node should be animated.  All nodes animated by default on animate enabled.  Non-animated nodes are positioned immediately when the layout starts
			ready: undefined, // callback on layoutready
			// stop: function() { window.removePendingRequest(); }, // callback on layoutstop
			// transform: function (node, position ){ return position; }, // transform a given node position. Useful for changing flow direction in discrete layouts
			// weaver: weaver
		});
		layout.run();
		this._cy.fit();
	}
	


}

