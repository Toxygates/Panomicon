import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { combineLatest, Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { IGeneSet } from 'src/app/shared/models/frontend-types.model';
import { UserDataService } from 'src/app/shared/services/user-data.service';

@Component({
  selector: 'app-gene-set-editor',
  templateUrl: './gene-set-editor.component.html',
  styleUrls: ['./gene-set-editor.component.scss']
})
export class GeneSetEditorComponent implements OnInit {

  constructor(private userData: UserDataService,
    private toastr: ToastrService,
    private route: ActivatedRoute, private router: Router) { }

  geneSetName$!: Observable<string | undefined>;
  geneSet$!: Observable<IGeneSet | undefined>;

  newProbesText = "";

  // selectedProbes: string[] | undefined;

  ngOnInit(): void {
    this.geneSetName$ = this.route.paramMap.pipe(
      map(params => params.get("geneSetName") as string | undefined));

    this.geneSet$ = combineLatest([
      this.userData.geneSets$,
      this.geneSetName$]).pipe(
        map(([geneSets, geneSetName]) => {
          return geneSetName ? geneSets.get(geneSetName) : undefined;
        })
      );
  }

  deleteProbes(geneSet: IGeneSet, probes: string[]): void {
    geneSet.probes = geneSet.probes.filter(probe => !probes.includes(probe));
    this.userData.geneSets$.value.set(geneSet.name, geneSet);
    this.userData.geneSets$.next(this.userData.geneSets$.value);
    this.toastr.success(`Deleted ${probes.join(", ")}`,
      `Deleted ${probes.length} probes`,);
  }

  addProbes(geneSet: IGeneSet, text: string): void {
    const candidates = [...new Set(text.split(/[\s,]+/))]
    const existing = new Set(geneSet.probes);
    const toAdd = candidates.filter(p => p.length > 0 && !existing.has(p));
    if (toAdd.length > 0) {
      geneSet.probes = geneSet.probes.concat(toAdd);
      this.toastr.success(`Added ${toAdd.join(", ")}`,
        `Added ${toAdd.length} probes`,);
      this.userData.geneSets$.value.set(geneSet.name, geneSet);
      this.userData.geneSets$.next(this.userData.geneSets$.value);
      this.newProbesText = "";
    } else {
      this.toastr.error("No new probes to add", "Error");
    }
  }

  deleteGeneSet(name: string): void {
    this.userData.geneSets$.value.delete(name);
    this.userData.geneSets$.next(this.userData.geneSets$.value);
    void this.router.navigate(['..'], { relativeTo: this.route });
  }
}
