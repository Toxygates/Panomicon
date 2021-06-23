import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '../shared.module';
import { ExpressionTableComponent } from './expression-table.component';
import { RouterModule } from '@angular/router';

@NgModule({
  imports: [
    CommonModule,
    SharedModule,
    RouterModule.forChild([
      { path: '', component: ExpressionTableComponent }
    ]),
  ],
  declarations: [
    ExpressionTableComponent
  ]
})
export class ExpressionTableModule { }
