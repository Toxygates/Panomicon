import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SampleFilteringComponent } from './sample-filtering.component';

describe('SampleFilterComponent', () => {
  let component: SampleFilteringComponent;
  let fixture: ComponentFixture<SampleFilteringComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ SampleFilteringComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(SampleFilteringComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    void expect(component).toBeTruthy();
  });
});
