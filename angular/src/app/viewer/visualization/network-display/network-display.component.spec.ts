import { ComponentFixture, TestBed } from '@angular/core/testing';

import { NetworkDisplayComponent } from './network-display.component';

describe('NetworkDisplayComponent', () => {
  let component: NetworkDisplayComponent;
  let fixture: ComponentFixture<NetworkDisplayComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ NetworkDisplayComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(NetworkDisplayComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
