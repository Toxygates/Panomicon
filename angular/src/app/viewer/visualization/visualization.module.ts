import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { NetworkDisplayComponent } from './network-display/network-display.component';
import { LayoutPickerComponent } from './layout-picker/layout-picker.component';
import { DisplayCanvasComponent } from './display-canvas/display-canvas.component';

@NgModule({
  declarations: [
    NetworkDisplayComponent,
    LayoutPickerComponent,
    DisplayCanvasComponent
  ],
  imports: [
    CommonModule,
		SharedModule,
		RouterModule.forChild([
			{
				path:'',
				component: NetworkDisplayComponent,
			}
		])
  ]
})
export class VisualizationModule { }
