import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LayoutPickerComponent } from './layout-picker.component';

describe('LayoutPickerComponent', () => {
  let component: LayoutPickerComponent;
  let fixture: ComponentFixture<LayoutPickerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ LayoutPickerComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(LayoutPickerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
