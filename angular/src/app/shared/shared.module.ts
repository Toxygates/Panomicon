import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BsDropdownModule } from 'ngx-bootstrap/dropdown';
import { CollapseModule } from 'ngx-bootstrap/collapse';
import { ForbiddenValueListValidatorDirective } from './directives/forbidden-value-list-validator.directive';
import { FocusInputDirective } from './directives/focus-input.directive';

@NgModule({
  declarations: [
    ForbiddenValueListValidatorDirective,
    FocusInputDirective
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
    FocusInputDirective,
  ]
})
export class SharedModule { }
