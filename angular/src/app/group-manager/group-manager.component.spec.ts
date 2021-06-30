import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BehaviorSubject } from 'rxjs';
import { ToastrService } from 'ngx-toastr';
import { UserDataService } from '../user-data.service';

import { GroupManagerComponent } from './group-manager.component';
import { FormsModule } from '@angular/forms';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { Component, Directive, NO_ERRORS_SCHEMA, Type } from '@angular/core';

class MockUserDataService {
  sampleGroups = {
    observable: new BehaviorSubject(new Map())
  }
  isAcceptableGroupName() {
    return false;
  }
  canSelectGroup() {
    return true;
  }
}
class MockToastrService {}

export function MockDirective(options: Component): Type<Directive> {
  const metadata: Directive = {
    selector: options.selector,
    inputs: options.inputs,
    outputs: options.outputs,
  };
  return Directive(metadata)(class MockDirectiveClass {});
}

describe('GroupManagerComponent', () => {
  let component: GroupManagerComponent;
  let fixture: ComponentFixture<GroupManagerComponent>;
  const mockUserData = new MockUserDataService();

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ FormsModule, BrowserAnimationsModule ],
      declarations: [
        GroupManagerComponent,
        MockDirective({
          selector: '[collapse]',
          inputs: ['collapse']
        })
      ],
      providers: [
        { provide: UserDataService, useValue: mockUserData },
        { provide: ToastrService, useClass: MockToastrService }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(GroupManagerComponent);
    component = fixture.componentInstance;
    mockUserData.sampleGroups.observable.next(
      new Map([["florb", {"name": "florb", "samples": [444, 555, 666],
                        "enabled": false}],
             ["spabble", {"name": "spabble", "samples": [111, 222, 333],
                          "enabled": true}],
            ]));
    component.currentRenamingGroup =  component.currentDeletingGroup = undefined;
    fixture.detectChanges();
  });

  it('should have headers for each group, in alphabetical order', () => {
    const groupManagerElement: HTMLElement = fixture.nativeElement as HTMLElement;
    const nodes = groupManagerElement.querySelectorAll('.card-title');
    const innerHTMLs = Array.from(nodes).map(n => n.innerHTML)
    void expect(innerHTMLs[0]).toContain('florb');
    void expect(innerHTMLs[1]).toContain('spabble');
  });

  it('should list sample IDs', () => {
    const groupManagerElement: HTMLElement = fixture.nativeElement as HTMLElement;
    const nodes = groupManagerElement.querySelectorAll('li');
    const innerHTMLs = Array.from(nodes).map(n => n.innerHTML)
    void expect(innerHTMLs[0]).toContain('444');
    void expect(innerHTMLs[2]).toContain('666');
    void expect(innerHTMLs[4]).toContain('222');
  });

  it('should update when sample groups change', () => {
    mockUserData.sampleGroups.observable.next(
      new Map([["barg", {"name": "barg", "samples": [444, 555, 666],
                "enabled": false}],
               ["slek", {"name": "slek", "samples": [111, 222, 333],
                "enabled": true}],
      ]));
    fixture.detectChanges();
    const groupManagerElement: HTMLElement = fixture.nativeElement as HTMLElement;
    const nodes = groupManagerElement.querySelectorAll('.card-title');
    const innerHTMLs = Array.from(nodes).map(n => n.innerHTML)
    void expect(innerHTMLs[0]).toContain('barg');
    void expect(innerHTMLs[1]).toContain('slek');
  })

  it('should expand renaming or deleting for one group at a time', () => {
    const groupManagerElement: HTMLElement = fixture.nativeElement as HTMLElement;
    const renameDivs = Array.from(groupManagerElement.querySelectorAll('div.renameCollapse'));
    const deleteDivs = Array.from(groupManagerElement.querySelectorAll('div.deleteCollapse'));
    const uncollapseds = function() {
      return renameDivs.concat(deleteDivs).map(b =>
        ((b as HTMLElement).getAttribute("ng-reflect-collapse") !== 'true' ? 1 : 0) as number
      );
    };

    void expect(uncollapseds().reduce((a,b)=>a+b)).toEqual(0);

    component.toggleRenamingGroup("florb");
    fixture.detectChanges();
    void expect(uncollapseds().reduce((a,b)=>a+b)).toEqual(1);
    void expect(uncollapseds()[0]).toEqual(1);

    component.toggleDeletingGroup("spabble");
    fixture.detectChanges();
    void expect(uncollapseds().reduce((a,b)=>a+b)).toEqual(1);
    void expect(uncollapseds()[3]).toEqual(1);
  })

});
