import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BsModalService } from 'ngx-bootstrap/modal';
import { GenesetMenuComponent } from './geneset-menu.component';

class MockModalService {}

describe('GenesetMenuComponent', () => {
  let component: GenesetMenuComponent;
  let fixture: ComponentFixture<GenesetMenuComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ GenesetMenuComponent ],
      providers: [
        { provide: BsModalService, useValue: MockModalService },
      ],
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(GenesetMenuComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    void expect(component).toBeTruthy();
  });
});
