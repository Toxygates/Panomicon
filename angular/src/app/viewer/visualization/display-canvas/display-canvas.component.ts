import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-display-canvas',
  templateUrl: './display-canvas.component.html',
  styleUrls: ['./display-canvas.component.scss']
})
export class DisplayCanvasComponent implements OnInit {

  constructor() { }

  ngOnInit(): void {
				// this._cy = cytoscape({ 
		// 	container: document.getElementById('cy'),
		// 	style: [
		// 		{
		// 			selector: 'node',
		// 			style: {
		// 				'label': 'data(label)',
		// 				'text-valign': 'center',
		// 				'text-halign': 'center',
		// 				'background-color': 'data(color)',
		// 				'border-color': 'data(borderColor)',
		// 				'border-width': '1px',
		// 				'display': 'element',
		// 			}
		// 		},
		// 		{
		// 			selector: "edge",
		// 			style :{
		// 				"line-color": "data(color)",
		// 			}
		// 		}
		// 	]
		// });
		// this._cy.add(this._net.getNodes());
		// this._cy.add(this._net.getInteractions());

		// let layout = this._cy.layout({name: 'concentric'});
		// layout.run();
		// this._cy.fit();
  }

}
