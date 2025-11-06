import { Component, Input, Output, EventEmitter } from '@angular/core';

@Component({
  selector: 'app-layout-picker',
  templateUrl: './layout-picker.component.html',
  styleUrls: ['./layout-picker.component.scss'],
})
export class LayoutPickerComponent {
  layouts = [
    { label: 'None', name: 'null', type: 'NullLayoutOptions' },
    { label: 'Random', name: 'random', type: 'RandomLayoutOptions' },
    { label: 'Preset', name: 'preset', type: 'PresetLayoutOptions' },
    { label: 'Grid', name: 'grid', type: 'GridLayoutOptions' },
    { label: 'Circle', name: 'circle', type: 'CircleLayoutOptions' },
    {
      label: 'Concentric',
      name: 'concentric',
      type: 'ConcentricLayoutOptions',
    },
    {
      label: 'Breadth-First',
      name: 'breadthfirst',
      type: 'BreadthFirstLayoutOptions',
    },
    { label: 'Cose', name: 'cose', type: 'CoseLayoutOptions' },
  ] as const;

  @Input()
  disabled = true;

  private _currentLayout = 'Concentric';
  get currentLayout(): string {
    return this._currentLayout;
  }

  @Output() changeLayoutEmitter = new EventEmitter<string>();

  changeLayout(options: Layout): void {
    this._currentLayout = options.label;
    this.changeLayoutEmitter.emit(options.name);
  }
}

interface Layout {
  label: string;
  name: string;
  type: string;
}
