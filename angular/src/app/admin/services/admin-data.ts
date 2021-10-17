import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { Batch } from './admin-types';
import { BackendService } from './backend.service';

@Injectable({
  providedIn: 'root'
})
export class AdminDataService {

  batches$: BehaviorSubject<Batch[] | null>

  constructor(private backend: BackendService) {
    this.batches$ = new BehaviorSubject<Batch[] | null>(null);
    this.backend.getBatches().subscribe(batches => this.batches$.next(batches));
  }

}
