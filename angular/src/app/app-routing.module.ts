import { NgModule } from '@angular/core';
import { Routes, RouterModule, PreloadAllModules } from '@angular/router';
import { PageNotFoundComponent } from './viewer/page-not-found/page-not-found.component';
import { ViewerComponent } from './viewer/viewer.component';

const routes: Routes = [
  {
    path: 'viewer',
    component: ViewerComponent, // this is the component with the <router-outlet> in the template
    children: [
      {
        path: 'sample-browser',
        loadChildren: () =>
          import('./viewer/sample-browser/sample-browser.module').then(
            (m) => m.SampleBrowserModule
          ),
      },
      {
        path: 'expression-table',
        loadChildren: () =>
          import('./viewer/expression-table/expression-table.module').then(
            (m) => m.ExpressionTableModule
          ),
      },
      {
        path: 'sample-groups',
        loadChildren: () =>
          import('./viewer/group-manager/group-manager.module').then(
            (m) => m.GroupManagerModule
          ),
      },
      {
        path: 'gene-sets',
        loadChildren: () =>
          import('./viewer/gene-sets/gene-sets.module').then(
            (m) => m.GeneSetsModule
          ),
      },
      {
        path: 'visualization',
        loadChildren: () =>
          import('./viewer/visualization/visualization.module').then(
            (m) => m.VisualizationModule
          ),
      },
      { path: '', redirectTo: 'sample-browser', pathMatch: 'full' },
      { path: '**', component: PageNotFoundComponent },
    ],
  },
  {
    path: 'admin',
    loadChildren: () =>
      import('./admin/admin.module').then((m) => m.AdminModule),
  },
  { path: '', redirectTo: 'viewer/sample-browser', pathMatch: 'full' },
  { path: '**', component: PageNotFoundComponent },
];

@NgModule({
  imports: [
    RouterModule.forRoot(routes, {
      relativeLinkResolution: 'legacy',
      preloadingStrategy: PreloadAllModules,
    }),
  ],
  exports: [RouterModule],
})
export class AppRoutingModule {}
