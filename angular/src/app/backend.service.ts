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

  getDatasets() {
    return this.http.get(this.serviceUrl + 'dataset')
      .pipe(
        tap(_ => console.log('fetched datasets')),
        catchError(error => {
          console.error('Error fetching datasets: ' + error);
          throw error;
        })
      );
  }

  getBatchesForDataset(datasetId: string): Observable<any[]> {
    return this.http.get<any[]>(this.serviceUrl + 'batch/dataset/'
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
    return this.http.get(this.serviceUrl + 'sample/batch/'
      + batchId)
      .pipe(
        tap(_ => console.log('fetched samples')),
        catchError(error => {
          console.error('Error fetching samples: ' + error);
          throw error;
        })
      )
  }

  getAttributesForBatch(batchId: string) {
    return this.http.get(this.serviceUrl + 'attribute/batch/'
      + batchId)
      .pipe(
        tap(_ => console.log('fetched attributes')),
        catchError(error => {
          console.error('Error fetching attributes: ' + error);
          throw error;
        })
      )
  }

  getAttributeValues(samples: string[], batches: string[], attributes: string[]): any {
    return this.http.post(this.serviceUrl + 'attributeValues',
      {
        "samples": samples,
        "batches": batches,
        "attributes": attributes,
      })
      .pipe(
        tap(_ => console.log('fetched attributes values')),
        catchError(error => {
          console.error('Error fetching attributes values ' + error);
          throw error;
        })
      )
  }
}
