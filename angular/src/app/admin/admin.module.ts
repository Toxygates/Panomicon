import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminComponent } from './admin/admin.component';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../viewer/shared/shared.module';
import { BatchesComponent } from './batches/batches.component';

@NgModule({
  declarations: [
    AdminComponent,
    BatchesComponent,
  ],
  imports: [
    CommonModule,
    SharedModule,
    RouterModule.forChild([
      {
        path: '',
        component: AdminComponent,
        children: [
          { path: 'batches', component: BatchesComponent },
          { path: '**',   redirectTo: 'batches', pathMatch: 'full' },
        ]
      }
    ]),
  ]
})
export class AdminModule { }
