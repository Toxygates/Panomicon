import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { HttpClientModule } from '@angular/common/http';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { ScrollingModule } from '@angular/cdk/scrolling';

import { AppRoutingModule } from './app-routing.module';

import { AppComponent } from './app.component';
import { DatasetPickerComponent } from './dataset-picker/dataset-picker.component';
import { BatchBrowserComponent } from './batch-browser/batch-browser.component';
import { BatchPickerComponent } from './batch-picker/batch-picker.component';
import { BatchSamplesComponent } from './batch-samples/batch-samples.component';

import { CardModule } from 'primeng/card';
import { MenubarModule } from 'primeng/menubar';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { ProgressSpinnerModule } from 'primeng/progressspinner';

@NgModule({
  declarations: [
    AppComponent,
    DatasetPickerComponent,
    BatchBrowserComponent,
    BatchPickerComponent,
    BatchSamplesComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    HttpClientModule,
    BrowserAnimationsModule,
    ScrollingModule,
    CardModule,
    MenubarModule,
    ButtonModule,
    TableModule,
    ProgressSpinnerModule,
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
