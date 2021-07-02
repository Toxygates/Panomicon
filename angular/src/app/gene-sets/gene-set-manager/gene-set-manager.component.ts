import { Component, Input, OnInit } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { IGeneSet } from 'src/app/shared/models/frontend-types.model';
import { UserDataService } from 'src/app/shared/services/user-data.service';

@Component({
  selector: 'app-gene-set-manager',
  templateUrl: './gene-set-manager.component.html',
  styleUrls: ['./gene-set-manager.component.scss']
})
export class GeneSetManagerComponent implements OnInit {

  constructor(private userData: UserDataService) { }

  @Input() geneSets$!: Observable<Map<string, IGeneSet>>
  platforms$!: Observable<Set<string>>;

  ngOnInit(): void {
    this.geneSets$ = this.userData.geneSets.observable;
    this.platforms$ = this.geneSets$.pipe(
      map(geneSetMap => {
        const platforms = new Set<string>();
        geneSetMap.forEach(geneSet => {
          platforms.add(geneSet.platform);
        });
        return platforms;
      })
    )
  }

  geneSetsForPlatform$(platform: string): Observable<IGeneSet[]> {
    return this.geneSets$.pipe(
      map(geneSets => {
        return Array.from(geneSets.values()).filter(geneSet => geneSet.platform == platform);
      })
    )
  }
}
