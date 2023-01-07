import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ToastrService } from 'ngx-toastr';

import { SampleFilteringComponent } from './sample-filtering.component';

class MockToastrService {}

describe('SampleFilteringComponent', () => {
  let component: SampleFilteringComponent;
  let fixture: ComponentFixture<SampleFilteringComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [SampleFilteringComponent],
      providers: [{ provide: ToastrService, useClass: MockToastrService }],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(SampleFilteringComponent);
    component = fixture.componentInstance;
    component.filters = [];
    fixture.detectChanges();
  });

  it('should create', () => {
    void expect(component).toBeTruthy();
  });
});
