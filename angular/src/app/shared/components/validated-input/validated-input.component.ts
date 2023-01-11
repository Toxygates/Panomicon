import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-validated-input',
  templateUrl: './validated-input.component.html',
  styleUrls: ['./validated-input.component.scss'],
})
export class ValidatedInputComponent {
  @Input() label!: string;
  @Input() id!: string;
  @Input() value!: string | undefined;
  @Output() valueChange = new EventEmitter();
  @Input() disabled = false;
  @Input() invalid = false;
  @Input() invalidMessage: string | undefined;
}
