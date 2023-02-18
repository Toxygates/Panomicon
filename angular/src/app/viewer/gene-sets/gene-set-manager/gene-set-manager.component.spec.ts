import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GeneSetManagerComponent } from './gene-set-manager.component';

describe('GeneSetManagerComponent', () => {
  let component: GeneSetManagerComponent;
  let fixture: ComponentFixture<GeneSetManagerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [GeneSetManagerComponent],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(GeneSetManagerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    void expect(component).toBeTruthy();
  });
});
