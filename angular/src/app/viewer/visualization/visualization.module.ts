import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { NetworkDisplayComponent } from './network-display/network-display.component';
import { LayoutPickerComponent } from './layout-picker/layout-picker.component';
import { DisplayCanvasComponent } from './display-canvas/display-canvas.component';
import { SampleGroupPickerComponent } from './sample-group-picker/sample-group-picker.component';
import { GeneSetPickerComponent } from './gene-set-picker/gene-set-picker.component';

@NgModule({
  declarations: [
    NetworkDisplayComponent,
    LayoutPickerComponent,
    DisplayCanvasComponent,
    SampleGroupPickerComponent,
    GeneSetPickerComponent,
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
