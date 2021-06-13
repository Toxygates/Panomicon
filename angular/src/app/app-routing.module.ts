import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { SampleBrowserComponent } from './sample-browser/sample-browser/sample-browser.component';
import { ExpressionTableComponent } from './expression-table/expression-table.component';
import { GroupManagerComponent } from './group-manager/group-manager.component';
import { PageNotFoundComponent } from './page-not-found/page-not-found.component';

const routes: Routes = [
  { path: 'sample-browser', component: SampleBrowserComponent },
  { path: 'expression-table', component: ExpressionTableComponent },
  { path: 'sample-groups', component: GroupManagerComponent },
  { path: '',   redirectTo: '/sample-browser', pathMatch: 'full' },
  { path: '**', component: PageNotFoundComponent }
];

@NgModule({
  imports: [RouterModule.forRoot(routes, { relativeLinkResolution: 'legacy' })],
  exports: [RouterModule]
})
export class AppRoutingModule { }
