import { CommonModule } from "@angular/common";
import { NgModule } from "@angular/core";
import { RouterModule } from "@angular/router";
import { SharedModule } from "../../shared/shared.module";
import { BatchPickerComponent } from "./batch-picker/batch-picker.component";
import { DatasetPickerComponent } from "./dataset-picker/dataset-picker.component";
import { GroupCreationComponent } from "./group-creation/group-creation.component";
import { SampleBrowserComponent } from "./sample-browser/sample-browser.component";
import { SampleFilteringComponent } from "./sample-filtering/sample-filtering.component";
import { SampleTableComponent } from "./sample-table/sample-table.component";
import { BatchStatisticsComponent } from './batch-statistics/batch-statistics.component';

@NgModule({
  imports: [
    CommonModule,
    SharedModule,
    RouterModule.forChild([
      {
        path: '',
        component: SampleBrowserComponent,
        children: [
          { path: 'table', component: SampleTableComponent },
          { path: 'batch-statistics', component: BatchStatisticsComponent },
          { path: '**',   redirectTo: 'table', pathMatch: 'full' },
        ]
      }
    ]),
  ],
  declarations: [
    SampleBrowserComponent,
    BatchPickerComponent,
    DatasetPickerComponent,
    GroupCreationComponent,
    SampleFilteringComponent,
    SampleTableComponent,
    BatchStatisticsComponent
  ]
})
export class SampleBrowserModule { }
