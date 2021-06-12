import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse  } from '@angular/common/http';
import { Observable } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { IAttribute, IBatch, IDataset, Sample } from './models/backend-types.model';

@Injectable({
  providedIn: 'root'
})
export class BackendService {

  constructor(private http: HttpClient) { }

  serviceUrl = 'json/';

  getDatasets(): Observable<IDataset[]> {
    return this.http.get<IDataset[]>(this.serviceUrl + 'dataset')
      .pipe(
        tap(() => console.log('fetched datasets')),
        catchError((error: HttpErrorResponse) => {
          console.error(`Error fetching datasets: ${error.message}`);
          throw error;
        })
      );
  }

  getBatchesForDataset(datasetId: string): Observable<IBatch[]> {
    return this.http.get<IBatch[]>(this.serviceUrl + 'batch/dataset/'
      + datasetId)
      .pipe(
        tap(() => console.log('fetched batches')),
        catchError((error: HttpErrorResponse) => {
          console.error(`Error fetching batches: ${error.message}`);
          throw error;
        })
      )
  }

  getSamplesForBatch(batchId: string): Observable<Sample[]> {
    return this.http.get<Sample[]>(this.serviceUrl + 'sample/batch/'
      + batchId)
      .pipe(
        tap(() => console.log('fetched samples')),
        catchError((error: HttpErrorResponse) => {
          console.error(`Error fetching samples: ${error.message}`);
          throw error;
        })
      )
  }

  getAttributesForBatch(batchId: string): Observable<IAttribute[]> {
    return this.http.get<IAttribute[]>(this.serviceUrl + 'attribute/batch/'
      + batchId)
      .pipe(
        tap(() => console.log('fetched attributes')),
        catchError((error: HttpErrorResponse) => {
          console.error(`Error fetching attributes: ${error.message}`);
          throw error;
        })
      )
  }

  getAttributeValues(samples: string[], batches: string[], attributes: string[]): Observable<Sample[]> {
    return this.http.post<Sample[]>(this.serviceUrl + 'attributeValues',
      {
        "samples": samples,
        "batches": batches,
        "attributes": attributes,
      })
      .pipe(
        tap(() => console.log('fetched attributes values')),
        catchError((error: HttpErrorResponse) => {
          console.error(`Error fetching attributes values: ${error.message}`);
          throw error;
        })
      )
  }
}
