import { Component, Input, OnInit } from '@angular/core';
import { Network } from 'src/app/shared/models/backend-types.model';
import cytoscape, {
  EdgeDataDefinition,
  NodeDataDefinition,
  Stylesheet,
} from 'cytoscape';

/** default colours for nodes in the graph */
const nodeColor = Object.freeze({
  mRNA: '#007f7f',
  microRNA: '#827f00',
  HIGHLIGHT: '#ffde4c',
  SELECTED: '#00de4c',
  CONNECTED: '#6fde95',
});

/** list of shapes that can be used to draw a node */
const nodeShape = Object.freeze({
  mRNA: 'ellipse',
  microRNA: 'pentagon',
});

@Component({
  selector: 'app-display-canvas',
  templateUrl: './display-canvas.component.html',
  styleUrls: ['./display-canvas.component.scss'],
})
export class DisplayCanvasComponent implements OnInit {
  private _fetching!: boolean;
  get fetching(): boolean {
    return this._fetching;
  }
  @Input()
  set fetching(fetching: boolean) {
    this._fetching = fetching;
  }

  private _colspan!: string;
  @Input()
  set colspan(colspan: string) {
    this._colspan = colspan;
  }
  get colspan(): string {
    return this._colspan;
  }

  protected _isEmpty = true;
  get isEmpty(): boolean {
    return this._isEmpty;
  }

  @Input()
  set network(net: Network | null) {
    if (net === null) {
      this._isEmpty = true;
      return;
    }
    this._isEmpty = false;
    const nodes: cytoscape.ElementDefinition[] = net.nodes.map((n) => {
      return {
        group: 'nodes',
        data: {
          id: n.id,
          type: n.type,
          symbols: n.symbols,
          weights: n.weights,
          color: n.type === 'mRNA' ? nodeColor.mRNA : nodeColor.microRNA,
          shape: n.type === 'mRNA' ? nodeShape.mRNA : nodeShape.microRNA,
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
    } as cytoscape.CytoscapeOptions);
    this.initStyle();
  }

  public updateLayout(name = '', boundingBox = undefined): void {
    const layout = this._cy.layout({
      name: name,
      fit: true, // whether to fit to viewport
      padding: 0, // fit padding
      boundingBox: boundingBox, // constrain layout bounds; { x1, y1, x2, y2 } or { x1, y1, w, h }
      animate: false, // whether to transition the node positions
    } as cytoscape.LayoutOptions);
    layout.run();
    this._cy.fit();
  }

  public initStyle(): void {
    if (this._cy.container() === null) return;

    const nodeStyle: Stylesheet = {
      selector: 'node',
      css: {
        label: 'data(label)',
        'text-valign': 'center',
        'text-halign': 'center',
        shape:
          'data(shape)' as cytoscape.Css.PropertyValueNode<cytoscape.Css.NodeShape>,
        'background-color': 'data(color)',
        'border-color': 'data(borderColor)',
        'border-width': '1px',
        display: 'element',
      },
    };

    this._cy.style([nodeStyle]);
  }
}
