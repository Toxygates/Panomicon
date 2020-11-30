import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { NetworkVizComponent } from './network-viz.component';

describe('NetworkVizComponent', () => {
  let component: NetworkVizComponent;
  let fixture: ComponentFixture<NetworkVizComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ NetworkVizComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(NetworkVizComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
