import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { GeneSetManagerComponent } from './gene-set-manager/gene-set-manager.component';
import { GeneSetEditorComponent } from './gene-set-editor/gene-set-editor.component';
import { PlatformPickerComponent } from './platform-picker/platform-picker.component';

@NgModule({
  imports: [
    CommonModule,
    SharedModule,
    RouterModule.forChild([
      {
        path: '',
        component: GeneSetManagerComponent,
        children: [
          { path: '', component: GeneSetEditorComponent },
          { path: ':geneSetName', component: GeneSetEditorComponent },
        ],
      },
    ]),
  ],
  declarations: [
    GeneSetManagerComponent,
    GeneSetEditorComponent,
    PlatformPickerComponent,
  ],
})
export class GeneSetsModule {}
