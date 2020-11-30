import * as cy from 'cytoscape';

import { Component, OnInit } from '@angular/core';

/**
 * Component description
 */
@Component({
  selector: 'app-network-viz',
  templateUrl: './network-viz.component.html',
  styleUrls: ['./network-viz.component.scss']
})

/**
 * TEST COMPONENT
 * Used for the implementation of basic network visualization, includint the
 * test integration of cytoscape.js as drawing library
 */
export class NetworkVizComponent implements OnInit {

  /** The current height, in pixels, of the canvas element used for display */
  protected _canvasHeight: number;

  /**
   * Constructor
   */
  constructor() { }

  /**
   * Init basic properties of the component
   */
  ngOnInit(): void {
    /* set the canvas height */
    this._canvasHeight = 500;
  }

  get canvasHeight(): number{
    return this._canvasHeight;
  }

}
