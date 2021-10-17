import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '../../shared/shared.module';
import { ExpressionTableComponent } from './expression-table/expression-table.component';
import { RouterModule } from '@angular/router';
import { GenesetMenuComponent } from './geneset-menu/geneset-menu.component';

@NgModule({
  imports: [
    CommonModule,
    SharedModule,
    RouterModule.forChild([
      { path: '', component: ExpressionTableComponent }
    ]),
  ],
  declarations: [
    ExpressionTableComponent,
    GenesetMenuComponent
  ]
})
export class ExpressionTableModule { }
