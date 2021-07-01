import { Directive, Input } from '@angular/core';
import { AbstractControl, NG_VALIDATORS, Validator } from '@angular/forms';

@Directive({
  selector: '[appForbiddenList]',
  providers: [{
      provide: NG_VALIDATORS,
      useExisting: ForbiddenValueListValidatorDirective,
      multi: true
  }]
})
export class ForbiddenValueListValidatorDirective implements Validator {
  @Input('appForbiddenList') excludedList: string[] = [];

  validate(control: AbstractControl): { [key: string]: unknown } | null {
      const value = control.value as string;
      return this.excludedList?.includes(value) ? {forbiddenValue: value} : null;
  }
}
