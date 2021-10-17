import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminComponent } from './admin/admin.component';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../shared/shared.module';
import { BatchesComponent } from './batches/batches.component';
import { PlatformsComponent } from './platforms/platforms.component';
import { DatasetsComponent } from './datasets/datasets.component';
import { InstancesComponent } from './instances/instances.component';

@NgModule({
  declarations: [
    AdminComponent,
    BatchesComponent,
    PlatformsComponent,
    DatasetsComponent,
    InstancesComponent,
  ],
  imports: [
    CommonModule,
    SharedModule,
    RouterModule.forChild([
      {
        path: '',
        component: AdminComponent,
        children: [
          { path: 'platforms', component: PlatformsComponent },
          { path: 'batches', component: BatchesComponent },
          { path: 'datasets', component: DatasetsComponent },
          { path: 'instances', component: InstancesComponent },
          { path: '**',   redirectTo: 'datasets', pathMatch: 'full' },
        ]
      }
    ]),
  ]
})
export class AdminModule { }
