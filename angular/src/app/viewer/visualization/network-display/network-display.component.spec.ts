import { HttpClient } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { NetworkDisplayComponent } from './network-display.component';

class MockService {}

describe('NetworkDisplayComponent', () => {
  let component: NetworkDisplayComponent;
  let fixture: ComponentFixture<NetworkDisplayComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ NetworkDisplayComponent ],
      providers: [
        { provide: HttpClient, useClass: MockService },
      ],
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(NetworkDisplayComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    void expect(component).toBeTruthy();
  });
});
