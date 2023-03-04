import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DisplayCanvasComponent } from './display-canvas.component';

describe('DisplayCanvasComponent', () => {
  let component: DisplayCanvasComponent;
  let fixture: ComponentFixture<DisplayCanvasComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [DisplayCanvasComponent],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DisplayCanvasComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    void expect(component).toBeTruthy();
  });
});
