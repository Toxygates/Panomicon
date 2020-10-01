import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class BackendService {

  constructor(private http: HttpClient) { }

  serviceUrl = 'json/';
  datasetsPath = 'dataset';
  batchesByDatasetPath = 'batches/dataset/';
  samplesByBatchPath = 'samples/batch/';

  getDatasets() {
    return this.http.get(this.serviceUrl + this.datasetsPath)
      .pipe(
        tap(_ => console.log('fetched datasets')),
        catchError(error => {
          console.error('Error fetching datasets: ' + error);
          throw error;
        })
      );
  }

  getBatchesForDataset(datasetId: string) {
    return this.http.get(this.serviceUrl + this.batchesByDatasetPath 
      + datasetId)
      .pipe(
        tap(_ => console.log('fetched batches')),
        catchError(error => {
          console.error('Error fetching batches: ' + error);
          throw error;
        })
      )
  }

  getSamplesForBatch(batchId: string) {
    return this.http.get(this.serviceUrl + this.samplesByBatchPath 
      + batchId)
      .pipe(
        tap(_ => console.log('fetched samples')),
        catchError(error => {
          console.error('Error fetching samples: ' + error);
          throw error;
        })
      )
  }
}
