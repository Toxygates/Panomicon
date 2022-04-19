import { ComponentFixture, TestBed } from '@angular/core/testing';

import { VisualizationCanvasComponent } from './visualization-canvas.component';

describe('VisualizationCanvasComponent', () => {
  let component: VisualizationCanvasComponent;
  let fixture: ComponentFixture<VisualizationCanvasComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ VisualizationCanvasComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(VisualizationCanvasComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
