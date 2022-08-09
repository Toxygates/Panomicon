import { Component, OnInit } from '@angular/core';
import * as cytoscape from 'cytoscape';

@Component({
  selector: 'app-visualization-canvas',
  templateUrl: './visualization-canvas.component.html',
  styleUrls: ['./visualization-canvas.component.scss']
})
export class VisualizationCanvasComponent implements OnInit {
	
	private _cy: any;

  constructor() {	}

  ngOnInit(): void {
		this._cy = cytoscape({
			container: document.getElementById('cy'),
			elements: [ // list of graph elements to start with
			{ // node a
				data: { id: 'a' }
			},
			{ // node b
				data: { id: 'b' }
			},
			{ // edge ab
				data: { id: 'ab', source: 'a', target: 'b' }
			}
		],
	
		style: [ // the stylesheet for the graph
			{
				selector: 'node',
				style: {
					'background-color': '#666',
					'label': 'data(id)'
				}
			},
	
			{
				selector: 'edge',
				style: {
					'width': 3,
					'line-color': '#ccc',
					'target-arrow-color': '#ccc',
					'target-arrow-shape': 'triangle',
					'curve-style': 'bezier'
				}
			}
		],
	
		layout: {
			name: 'grid',
			rows: 1
		}
		}); 
  }

}
