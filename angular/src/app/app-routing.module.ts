import { NgModule } from '@angular/core';
import { Routes, RouterModule, PreloadAllModules } from '@angular/router';
import { PageNotFoundComponent } from './page-not-found/page-not-found.component';

const routes: Routes = [
  { path: 'sample-browser', loadChildren: () =>
    import('./sample-browser/sample-browser.module').then(m => m.SampleBrowserModule) },
  { path: 'expression-table', loadChildren: () =>
    import('./expression-table/expression-table.module').then(m => m.ExpressionTableModule) },
  { path: 'sample-groups', loadChildren: () =>
    import('./group-manager/group-manager.module').then(m => m.GroupManagerModule) },
  { path: '',   redirectTo: '/sample-browser', pathMatch: 'full' },
  { path: '**', component: PageNotFoundComponent }
];

@NgModule({
  imports: [RouterModule.forRoot(routes,
    {
      relativeLinkResolution: 'legacy',
      preloadingStrategy: PreloadAllModules
    })],
  exports: [RouterModule]
})
export class AppRoutingModule { }
