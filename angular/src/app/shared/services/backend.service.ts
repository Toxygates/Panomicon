import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse  } from '@angular/common/http';
import { Observable } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { Attribute, Batch, Dataset, Sample } from '../models/backend-types.model';
import { environment } from 'src/environments/environment';

@Injectable({
  providedIn: 'root'
})
export class BackendService {

  constructor(private http: HttpClient) { }

  serviceUrl = environment.apiUrl;

  getDatasets(): Observable<Dataset[]> {
    return this.http.get<Dataset[]>(this.serviceUrl + 'dataset')
      .pipe(
        tap(() => console.log('fetched datasets')),
        catchError((error: HttpErrorResponse) => {
          console.error(`Error fetching datasets: ${error.message}`);
          throw error;
        })
      );
  }

  getBatchesForDataset(datasetId: string): Observable<Batch[]> {
    return this.http.get<Batch[]>(this.serviceUrl + 'batch/dataset/'
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

  getAttributesForBatch(batchId: string): Observable<Attribute[]> {
    return this.http.get<Attribute[]>(this.serviceUrl + 'attribute/batch/'
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

  getRoles(): Observable<string[]> {
    return this.http.get<string[]>(this.serviceUrl + 'roles')
      .pipe(
        tap(() => console.log('fetched roles')),
        catchError((error: HttpErrorResponse) => {
          console.log(`Error fetching roles: ${error.message}`)
          throw error;
        })
      )
  }
}
