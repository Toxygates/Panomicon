import { Component, Input, OnInit } from '@angular/core';
import { Network } from 'src/app/shared/models/backend-types.model';
import cytoscape, { EdgeDataDefinition, NodeDataDefinition } from 'cytoscape';

@Component({
  selector: 'app-display-canvas',
  templateUrl: './display-canvas.component.html',
  styleUrls: ['./display-canvas.component.scss'],
})
export class DisplayCanvasComponent implements OnInit {
  @Input()
  set network(net: Network | null) {
    if (net === null) return;
    const nodes: cytoscape.ElementDefinition[] = net.nodes.map((n) => {
      return {
        group: 'nodes',
        data: {
          id: n.id,
          type: n.type,
          symbols: n.symbols,
          weights: n.weights,
        } as NodeDataDefinition,
      } as cytoscape.ElementDefinition;
    });
    this._cy.add(nodes);

    const edges: cytoscape.ElementDefinition[] = net.interactions.map((i) => {
      return {
        group: 'edges',
        data: {
          id: i.from + i.to,
          source: i.from,
          target: i.to,
          label: i.label,
          weight: i.weight,
        } as EdgeDataDefinition,
      } as cytoscape.ElementDefinition;
    });
    this._cy.add(edges);

    const layout = this._cy.layout({ name: 'concentric' });
    layout.run();
    this._cy.fit();
  }

  private _cy!: cytoscape.Core;

  ngOnInit(): void {
    this._cy = cytoscape({
      container: document.getElementById('cy'),
      style: [
        {
          selector: 'node',
          style: {
            label: 'data(label)',
            'text-valign': 'center',
            'text-halign': 'center',
            'background-color': 'data(color)',
            'border-color': 'data(borderColor)',
            'border-width': '1px',
            display: 'element',
          },
        },
        {
          selector: 'edge',
          style: {
            'line-color': 'data(color)',
          },
        },
      ],
    } as cytoscape.CytoscapeOptions);
  }

  public updateLayout(name = '', boundingBox = undefined): void {
    const layout = this._cy.layout({
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
    } as cytoscape.LayoutOptions);
    layout.run();
    this._cy.fit();
  }
}
