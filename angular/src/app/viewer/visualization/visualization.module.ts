import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { VisualizationRoutingModule } from './visualization-routing.module';
import { VisualizationCanvasComponent } from './visualization-canvas/visualization-canvas.component';

@NgModule({
  declarations: [
    VisualizationCanvasComponent
  ],
  imports: [
    CommonModule,
		VisualizationRoutingModule
  ]
})
export class VisualizationModule { }
