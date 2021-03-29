import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';

import { AppRoutingModule } from './app-routing.module';
import { HttpClientModule } from '@angular/common/http';
import { FormsModule } from '@angular/forms';

import { ModalModule } from 'ngx-bootstrap/modal';
import { CollapseModule } from 'ngx-bootstrap/collapse';
import { AccordionModule } from 'ngx-bootstrap/accordion';
import { ToastrModule } from 'ngx-toastr';

import { AppComponent } from './app.component';
import { DatasetPickerComponent } from './dataset-picker/dataset-picker.component';
import { BatchBrowserComponent } from './batch-browser/batch-browser.component';
import { BatchPickerComponent } from './batch-picker/batch-picker.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { BatchSamplesComponent } from './batch-samples/batch-samples.component';
import { PageNotFoundComponent } from './page-not-found/page-not-found.component';
import { ExpressionTableComponent } from './expression-table/expression-table.component';
import { GroupManagerComponent } from './group-manager/group-manager.component';
import { SampleSearchComponent } from './sample-search/sample-search.component';

@NgModule({
  declarations: [
    AppComponent,
    DatasetPickerComponent,
    BatchBrowserComponent,
    BatchPickerComponent,
    BatchSamplesComponent,
    PageNotFoundComponent,
    ExpressionTableComponent,
    GroupManagerComponent,
    SampleSearchComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    HttpClientModule,
    BrowserAnimationsModule,
    FormsModule,
    ModalModule.forRoot(),
    CollapseModule.forRoot(),
    AccordionModule.forRoot(),
    ToastrModule.forRoot({
      timeOut: 4000,
    }),
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
