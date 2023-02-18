import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '../../shared/shared.module';
import { RouterModule } from '@angular/router';
import { GroupManagerComponent } from './group-manager.component';

@NgModule({
  imports: [
    CommonModule,
    SharedModule,
    RouterModule.forChild([{ path: '', component: GroupManagerComponent }]),
  ],
  declarations: [GroupManagerComponent],
})
export class GroupManagerModule {}
