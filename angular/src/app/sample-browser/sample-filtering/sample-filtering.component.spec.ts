import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BsModalService } from 'ngx-bootstrap/modal';
import { Subject } from 'rxjs';

import { SampleFilteringComponent } from './sample-filtering.component';

describe('SampleFilteringComponent', () => {
  let component: SampleFilteringComponent;
  let fixture: ComponentFixture<SampleFilteringComponent>;

  class MockModalService {}

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ SampleFilteringComponent ],
      providers: [ { provide: BsModalService, useClass: MockModalService } ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(SampleFilteringComponent);
    component = fixture.componentInstance;
    component.openModalObservable = new Subject<void>();
    component.filters = [];
    fixture.detectChanges();
  });

  it('should create', () => {
    void expect(component).toBeTruthy();
  });
});
