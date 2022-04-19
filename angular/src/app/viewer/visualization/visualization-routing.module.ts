import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { VisualizationCanvasComponent } from './visualization-canvas/visualization-canvas.component';

const routes: Routes = [
	{ 
		path: '',
		component: VisualizationCanvasComponent
	}
]

@NgModule({
	imports: [RouterModule.forChild(routes)],
	exports: [RouterModule]
})
export class VisualizationRoutingModule { }