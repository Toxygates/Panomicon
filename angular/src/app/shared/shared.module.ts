import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BsDropdownModule } from 'ngx-bootstrap/dropdown';
import { CollapseModule } from 'ngx-bootstrap/collapse';
import { ForbiddenValueListValidatorDirective } from './forbidden-value-list-validator.directive';

@NgModule({
  declarations: [
    ForbiddenValueListValidatorDirective
  ],
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
    ForbiddenValueListValidatorDirective,
  ]
})
export class SharedModule { }
