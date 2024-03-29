import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { Sample } from '../models/backend-types.model';
import { GeneSet, SampleGroup } from '../models/frontend-types.model';
import { SampleGroupLogic } from '../models/sample-group-logic.class';

@Injectable({
  providedIn: 'root',
})
export class UserDataService {
  sampleGroups$: BehaviorSubject<Map<string, SampleGroup>>;
  geneSets$: BehaviorSubject<Map<string, GeneSet>>;

  selectedDataset$: BehaviorSubject<string | null>;
  selectedBatch$: BehaviorSubject<string | null>;

  targetMineUsername$ = new BehaviorSubject('');
  targetMinePassword$ = new BehaviorSubject('');

  private enabledGroupsBehaviorSubject: BehaviorSubject<SampleGroup[]>;
  enabledGroups$: Observable<SampleGroup[]>;
  platform$: Observable<string | undefined>;

  firstNetworkSampleGroupName$: BehaviorSubject<string | null>;
  secondNetworkSampleGroupName$: BehaviorSubject<string | null>;
  networkGeneSetName$: BehaviorSubject<string | null>;

  static readonly SELECTED_DATASET_KEY: string = 'selectedDataset_v1';
  static readonly SELECTED_BATCH_KEY: string = 'selectedBatch_v1';
  static readonly SAMPLE_GROUPS_KEY: string = 'sampleGroups_v2';
  static readonly GENE_SETS_KEY: string = 'geneSets_v1';

  constructor() {
    this.sampleGroups$ = new BehaviorSubject(
      deserializeArray(UserDataService.SAMPLE_GROUPS_KEY)
    );
    this.sampleGroups$.subscribe((newValue) => {
      const json = JSON.stringify(Array.from(newValue));
      window.localStorage.setItem(UserDataService.SAMPLE_GROUPS_KEY, json);
    });

    this.geneSets$ = new BehaviorSubject(
      deserializeArray(UserDataService.GENE_SETS_KEY)
    );
    this.geneSets$.subscribe((newValue) => {
      const json = JSON.stringify(Array.from(newValue));
      window.localStorage.setItem(UserDataService.GENE_SETS_KEY, json);
    });

    this.selectedDataset$ = new BehaviorSubject(
      window.localStorage.getItem(UserDataService.SELECTED_DATASET_KEY)
    );
    this.selectedDataset$.subscribe((newValue) => {
      if (newValue) {
        window.localStorage.setItem(
          UserDataService.SELECTED_DATASET_KEY,
          newValue
        );
      }
    });

    this.selectedBatch$ = new BehaviorSubject(
      window.localStorage.getItem(UserDataService.SELECTED_BATCH_KEY)
    );
    this.selectedBatch$.subscribe((newValue) => {
      if (newValue) {
        window.localStorage.setItem(
          UserDataService.SELECTED_BATCH_KEY,
          newValue
        );
      }
    });

    this.enabledGroupsBehaviorSubject = this.enabledGroups$ =
      new BehaviorSubject([] as SampleGroup[]);
    this.sampleGroups$
      .pipe(
        map((value) =>
          Array.from(value.values()).filter((group) => group.enabled)
        )
      )
      .subscribe(this.enabledGroupsBehaviorSubject);
    this.platform$ = this.enabledGroups$.pipe(
      map((groups) => (groups.length > 0 ? groups[0].platform : undefined))
    );

    this.firstNetworkSampleGroupName$ = new BehaviorSubject<string | null>(
      null
    );
    this.secondNetworkSampleGroupName$ = new BehaviorSubject<string | null>(
      null
    );
    this.networkGeneSetName$ = new BehaviorSubject<string | null>(null);
  }

  canSelectGroup(group: SampleGroup): boolean {
    return SampleGroupLogic.canSelectGroup(
      group,
      this.enabledGroupsBehaviorSubject.value
    );
  }

  saveSampleGroup(name: string, samples: Sample[]): void {
    const newGroup = SampleGroupLogic.createSampleGroup(
      name,
      samples,
      this.enabledGroupsBehaviorSubject.value
    );
    this.sampleGroups$.value.set(name, newGroup);
    this.sampleGroups$.next(this.sampleGroups$.value);
  }
}

function deserializeArray<T>(key: string): Map<string, T> {
  const json = window.localStorage.getItem(key);
  const parsed = json ? (JSON.parse(json) as unknown) : [];
  const array = Array.isArray(parsed) ? parsed : [];
  return new Map<string, T>(array);
}

export function renameItem(
  itemMap: Map<string, { name: string }>,
  oldName: string,
  newName: string
): void {
  const item = itemMap.get(oldName);
  if (!item) throw new Error(`Tried to rename nonexistent item ${oldName}`);
  item.name = newName;
  itemMap.set(item.name, item);
  itemMap.delete(oldName);
}
