import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';

import { AppRoutingModule } from './app-routing.module';
import { HttpClientModule } from '@angular/common/http';

import { AppComponent } from './app.component';
import { DatasetPickerComponent } from './dataset-picker/dataset-picker.component';
import { BatchSelectorComponent } from './batch-selector/batch-selector.component';
import { BatchPickerComponent } from './batch-picker/batch-picker.component';



@NgModule({
  declarations: [
    AppComponent,
    DatasetPickerComponent,
    BatchSelectorComponent,
    BatchPickerComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    HttpClientModule
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
