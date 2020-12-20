import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';

import { AppRoutingModule } from './app-routing.module';
import { HttpClientModule } from '@angular/common/http';
import { FormsModule } from '@angular/forms';

import { ModalModule } from 'ngx-bootstrap/modal';
import { CollapseModule } from 'ngx-bootstrap/collapse';

import { AppComponent } from './app.component';
import { DatasetPickerComponent } from './dataset-picker/dataset-picker.component';
import { BatchBrowserComponent } from './batch-browser/batch-browser.component';
import { BatchPickerComponent } from './batch-picker/batch-picker.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { BatchSamplesComponent } from './batch-samples/batch-samples.component';
import { PageNotFoundComponent } from './page-not-found/page-not-found.component';
import { ExpressionTableComponent } from './expression-table/expression-table.component';

@NgModule({
  declarations: [
    AppComponent,
    DatasetPickerComponent,
    BatchBrowserComponent,
    BatchPickerComponent,
    BatchSamplesComponent,
    PageNotFoundComponent,
    ExpressionTableComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    HttpClientModule,
    BrowserAnimationsModule,
    FormsModule,
    ModalModule.forRoot(),
    CollapseModule.forRoot(),
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
