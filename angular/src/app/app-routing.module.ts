import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { BatchBrowserComponent } from './batch-browser/batch-browser.component';
import { ExpressionTableComponent } from './expression-table/expression-table.component';
import { PageNotFoundComponent } from './page-not-found/page-not-found.component';

const routes: Routes = [
  { path: 'batch-browser', component: BatchBrowserComponent },
  { path: 'expression-table', component: ExpressionTableComponent },
  { path: '',   redirectTo: '/batch-browser', pathMatch: 'full' },
  { path: '**', component: PageNotFoundComponent }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
