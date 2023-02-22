import { Injectable } from '@angular/core';
import {
  HttpClient,
  HttpErrorResponse,
  HttpResponse,
} from '@angular/common/http';
import { Observable } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import {
  Attribute,
  Batch,
  Dataset,
  Sample,
  GeneSet,
  Platform,
} from '../models/backend-types.model';
import { environment } from 'src/environments/environment';
import { GeneSet as FrontendGeneSet } from '../models/frontend-types.model';

@Injectable({
  providedIn: 'root',
})
export class BackendService {
  constructor(private http: HttpClient) {}

  serviceUrl = environment.apiUrl;

  getDatasets(): Observable<Dataset[]> {
    return this.http.get<Dataset[]>(this.serviceUrl + 'dataset').pipe(
      tap(() => console.log('fetched datasets')),
      catchError((error: HttpErrorResponse) => {
        console.error(`Error fetching datasets: ${error.message}`);
        throw error;
      })
    );
  }

  getBatchesForDataset(datasetId: string): Observable<Batch[]> {
    return this.http
      .get<Batch[]>(this.serviceUrl + 'batch/dataset/' + datasetId)
      .pipe(
        tap(() => console.log('fetched batches')),
        catchError((error: HttpErrorResponse) => {
          console.error(`Error fetching batches: ${error.message}`);
          throw error;
        })
      );
  }

  getSamplesForBatch(batchId: string): Observable<Sample[]> {
    return this.http
      .get<Sample[]>(this.serviceUrl + 'sample/batch/' + batchId)
      .pipe(
        tap(() => console.log('fetched samples')),
        catchError((error: HttpErrorResponse) => {
          console.error(`Error fetching samples: ${error.message}`);
          throw error;
        })
      );
  }

  getAttributesForBatch(batchId: string): Observable<Attribute[]> {
    return this.http
      .get<Attribute[]>(this.serviceUrl + 'attribute/batch/' + batchId)
      .pipe(
        tap(() => console.log('fetched attributes')),
        catchError((error: HttpErrorResponse) => {
          console.error(`Error fetching attributes: ${error.message}`);
          throw error;
        })
      );
  }

  getAttributeValues(
    samples: string[],
    batches: string[],
    attributes: string[]
  ): Observable<Sample[]> {
    return this.http
      .post<Sample[]>(this.serviceUrl + 'attributeValues', {
        samples: samples,
        batches: batches,
        attributes: attributes,
      })
      .pipe(
        tap(() => console.log('fetched attributes values')),
        catchError((error: HttpErrorResponse) => {
          console.error(`Error fetching attributes values: ${error.message}`);
          throw error;
        })
      );
  }

  getRoles(): Observable<string[]> {
    return this.http.get<string[]>(this.serviceUrl + 'roles').pipe(
      tap(() => console.log('fetched roles')),
      catchError((error: HttpErrorResponse) => {
        console.log(`Error fetching roles: ${error.message}`);
        throw error;
      })
    );
  }

  getPlatforms(): Observable<Platform[]> {
    return this.http.get<Platform[]>(this.serviceUrl + 'platform/user').pipe(
      tap(() => console.log('fetched platforms')),
      catchError((error: HttpErrorResponse) => {
        console.log(`Error fetching platforms: ${error.message}`);
        throw error;
      })
    );
  }

  deleteBatch(batchId: string): Observable<string> {
    console.log(this.serviceUrl + 'batch/' + batchId);
    return this.http.delete<string>(this.serviceUrl + 'batch/' + batchId).pipe(
      tap(() => console.log('deleted batch')),
      catchError((error: HttpErrorResponse) => {
        console.log(`Error deleting batch: ${error.message}`);
        throw error;
      })
    );
  }

  uploadFile(file: File): Observable<string> {
    const formData: FormData = new FormData();
    formData.append('fileKey', file, file.name);
    return this.http
      .post(this.serviceUrl + 'upload', formData, { responseType: 'text' })
      .pipe(
        tap(() => console.log('uploaded file')),
        catchError((error: HttpErrorResponse) => {
          console.log(`Error uploading file: ${error.message}`);
          throw error;
        })
      );
  }

  exportGeneSet(
    username: string,
    password: string,
    replace: boolean,
    geneSet: FrontendGeneSet
  ): Observable<HttpResponse<string>> {
    const url =
      this.serviceUrl +
      `intermine/list?user=${username}&pass=${password}&replace=${replace.toString()}`;
    const data = [
      {
        name: geneSet.name,
        items: geneSet.probes,
      },
    ];
    return this.http
      .post<HttpResponse<string>>(url, data, {
        headers: {
          'Content-Type': 'application/json',
        },
      })
      .pipe(
        tap(() => console.log('exported gene set')),
        catchError((error: HttpErrorResponse) => {
          console.log(`Error exporting gene set: ${error.message}`);
          throw error;
        })
      );
  }

  importGeneSets(
    username: string,
    password: string,
    platform: string
  ): Observable<GeneSet[]> {
    const url =
      this.serviceUrl +
      `intermine/list?user=${username}&pass=${password}&platform=${platform}`;
    return this.http.get<GeneSet[]>(url).pipe(
      tap(() => console.log('imported gene sets')),
      catchError((error: HttpErrorResponse) => {
        console.log(`Error exporting gene set: ${error.message}`);
        throw error;
      })
    );
  }

  logout(): Observable<string> {
    return this.http.get<string>(this.serviceUrl + 'logout').pipe(
      tap(() => console.log('logged out')),
      catchError((error: HttpErrorResponse) => {
        console.log(`Error logging out: ${error.message}`);
        throw error;
      })
    );
  }
}
