import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { of } from 'rxjs';

import { GeneSetEditorComponent } from './gene-set-editor.component';

class MockService {}
class MockActivatedRoute {
  paramMap = { pipe: () => of({geneSetName: "some gene set"}) }
}

describe('GeneSetEditorComponent', () => {
  let component: GeneSetEditorComponent;
  let fixture: ComponentFixture<GeneSetEditorComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ GeneSetEditorComponent ],
      providers: [
        { provide: ToastrService, useClass: MockService },
        { provide: ActivatedRoute, useClass: MockActivatedRoute },
        { provide: Router, useClass: MockService },
      ],
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(GeneSetEditorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    void expect(component).toBeTruthy();
  });
});
