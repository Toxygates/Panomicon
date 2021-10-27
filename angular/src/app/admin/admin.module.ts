import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminComponent } from './admin/admin.component';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../shared/shared.module';
import { BatchesComponent } from './batches/batches.component';
import { PlatformsComponent } from './platforms/platforms.component';
import { DatasetsComponent } from './datasets/datasets.component';
import { InstancesComponent } from './instances/instances.component';
import { EditDatasetComponent } from './edit-dataset/edit-dataset.component';
import { EditBatchComponent } from './edit-batch/edit-batch.component';
import { EditPlatformComponent } from './edit-platform/edit-platform.component';
import { EditInstanceComponent } from './edit-instance/edit-instance.component';

@NgModule({
  declarations: [
    AdminComponent,
    BatchesComponent,
    PlatformsComponent,
    DatasetsComponent,
    InstancesComponent,
    EditDatasetComponent,
    EditBatchComponent,
    EditPlatformComponent,
    EditInstanceComponent,
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
          { path: 'platforms/edit/:id', component: EditPlatformComponent },
          { path: 'platforms/add', component: EditPlatformComponent },
          { path: 'batches', component: BatchesComponent },
          { path: 'batches/edit/:id', component: EditBatchComponent },
          { path: 'batches/add', component: EditBatchComponent },
          { path: 'datasets/edit/:id', component: EditDatasetComponent },
          { path: 'datasets/add', component: EditDatasetComponent },
          { path: 'datasets', component: DatasetsComponent },
          { path: 'instances', component: InstancesComponent },
          { path: 'instances/edit/:id', component: EditInstanceComponent },
          { path: 'instances/add', component: EditInstanceComponent },
          { path: '**',   redirectTo: 'datasets', pathMatch: 'full' },
        ]
      }
    ]),
  ]
})
export class AdminModule { }
