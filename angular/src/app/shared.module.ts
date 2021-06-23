import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BsDropdownModule } from 'ngx-bootstrap/dropdown';
import { CollapseModule } from 'ngx-bootstrap/collapse';

@NgModule({
  declarations: [],
  imports: [
    CommonModule,
    FormsModule,
    BsDropdownModule,
    CollapseModule,
  ],
  exports: [
    CommonModule,
    FormsModule,
    BsDropdownModule,
    CollapseModule,
  ]
})
export class SharedModule { }
