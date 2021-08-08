import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';

import { AppRoutingModule } from './app-routing.module';
import { HttpClientModule } from '@angular/common/http';

import { ModalModule } from 'ngx-bootstrap/modal';
import { ToastrModule } from 'ngx-toastr';

import { AppComponent } from './app.component';
import { PageNotFoundComponent } from './viewer/page-not-found/page-not-found.component';
import { SharedModule } from './viewer/shared/shared.module';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { ViewerComponent } from './viewer/viewer.component';

@NgModule({
  declarations: [
    AppComponent,
    ViewerComponent,
    PageNotFoundComponent,
  ],
  imports: [
    BrowserModule,
    SharedModule,
    BrowserAnimationsModule,
    AppRoutingModule,
    HttpClientModule,
    ModalModule.forRoot(),
    ToastrModule.forRoot({
      timeOut: 4000,
    }),
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
