import { Directive, ElementRef, Input } from '@angular/core';

@Directive({
  selector: '[appFocusInput]'
})
export class FocusInputDirective {

  private value = false;
  @Input() public set appFocusInput(value: boolean) {
    if (!this.value && value) {
      this.focus();
    }
    this.value = value;
  }

  constructor(private element: ElementRef) { }

  focus(): void {
    setTimeout(()=>{
      (this.element.nativeElement as HTMLInputElement).focus();
    },50);
  }

}
