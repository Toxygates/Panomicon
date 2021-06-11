import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';

import { AppRoutingModule } from './app-routing.module';
import { HttpClientModule } from '@angular/common/http';
import { FormsModule } from '@angular/forms';

import { ModalModule } from 'ngx-bootstrap/modal';
import { CollapseModule } from 'ngx-bootstrap/collapse';
import { AccordionModule } from 'ngx-bootstrap/accordion';
import { BsDropdownModule } from 'ngx-bootstrap/dropdown';
import { ToastrModule } from 'ngx-toastr';

import { AppComponent } from './app.component';
import { DatasetPickerComponent } from './sample-browser/dataset-picker/dataset-picker.component';
import { SampleBrowserComponent } from './sample-browser/sample-browser/sample-browser.component';
import { BatchPickerComponent } from './sample-browser/batch-picker/batch-picker.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { PageNotFoundComponent } from './page-not-found/page-not-found.component';
import { ExpressionTableComponent } from './expression-table/expression-table.component';
import { GroupManagerComponent } from './group-manager/group-manager.component';
import { SampleTableComponent } from './sample-browser/sample-table/sample-table.component';
import { SampleFilteringComponent } from './sample-browser/sample-filtering/sample-filtering.component';

@NgModule({
  declarations: [
    AppComponent,
    DatasetPickerComponent,
    SampleBrowserComponent,
    BatchPickerComponent,
    PageNotFoundComponent,
    ExpressionTableComponent,
    GroupManagerComponent,
    SampleTableComponent,
    SampleFilteringComponent
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
    BsDropdownModule.forRoot(),
    ToastrModule.forRoot({
      timeOut: 4000,
    }),
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
