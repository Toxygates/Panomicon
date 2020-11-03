import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';

import { AppRoutingModule } from './app-routing.module';
import { HttpClientModule } from '@angular/common/http';
import { FormsModule } from '@angular/forms';

import { AppComponent } from './app.component';
import { DatasetPickerComponent } from './dataset-picker/dataset-picker.component';
import { BatchBrowserComponent } from './batch-browser/batch-browser.component';
import { BatchPickerComponent } from './batch-picker/batch-picker.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { BatchSamplesComponent } from './batch-samples/batch-samples.component';



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
    FormsModule
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
