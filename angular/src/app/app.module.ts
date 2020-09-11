import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';

import { AppRoutingModule } from './app-routing.module';
import { HttpClientModule } from '@angular/common/http';

import { AppComponent } from './app.component';
import { DatasetPickerComponent } from './dataset-picker/dataset-picker.component';
import { BatchSelectorComponent } from './batch-selector/batch-selector.component';
import { BatchPickerComponent } from './batch-picker/batch-picker.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { BatchSamplesComponent } from './batch-samples/batch-samples.component';



@NgModule({
  declarations: [
    AppComponent,
    DatasetPickerComponent,
    BatchSelectorComponent,
    BatchPickerComponent,
    BatchSamplesComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    HttpClientModule,
    BrowserAnimationsModule,
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
