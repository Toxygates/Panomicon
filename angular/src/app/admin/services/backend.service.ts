import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse  } from '@angular/common/http';
import { Observable } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { environment } from 'src/environments/environment';
import { Batch, Instance, Dataset, Platform } from './admin-types';

@Injectable({
  providedIn: 'root'
})
export class BackendService {

  constructor(private http: HttpClient) { }

  serviceUrl = environment.apiUrl;

  getPlatforms(): Observable<Platform[]> {
    return this.http.get<Platform[]>(this.serviceUrl + 'platform')
      .pipe(
        tap(() => console.log('fetched platforms')),
        catchError((error: HttpErrorResponse) => {
          console.error(`Error fetching platforms: ${error.message}`);
          throw error;
        })
      );
  }

  getBatches(): Observable<Batch[]> {
    return this.http.get<Batch[]>(this.serviceUrl + 'batch')
      .pipe(
        tap(() => console.log('fetched batches')),
        catchError((error: HttpErrorResponse) => {
          console.error(`Error fetching batches: ${error.message}`);
          throw error;
        })
      );
  }

  getDatasets(): Observable<Dataset[]> {
    return this.http.get<Dataset[]>(this.serviceUrl + 'dataset/all')
      .pipe(
        tap(() => console.log('fetched datasets')),
        catchError((error: HttpErrorResponse) => {
          console.error(`Error fetching datasets: ${error.message}`);
          throw error;
        })
      );
  }

  getInstances(): Observable<Instance[]> {
    return this.http.get<Instance[]>(this.serviceUrl + 'instance')
      .pipe(
        tap(() => console.log('fetched instances')),
        catchError((error: HttpErrorResponse) => {
          console.error(`Error fetching instances: ${error.message}`);
          throw error;
        })
      );
  }

  addBatch(batch: Partial<Batch>, files: Map<string, File>): Observable<string> {
    const formData: FormData = new FormData();
    for (const [key, value] of Object.entries(batch)) {
      formData.append(key, value?.toString() || "");
    }
    formData.append('metadata', files.get('metadata') as File);
    formData.append('exprData', files.get('exprData') as File);
    if (files.get('callsData')) {
      formData.append('callsData', files.get('callsData') as File);
    }
    if (files.get('probesData')) {
      formData.append('probesData', files.get('probesData') as File);
    }
    return this.http.post(this.serviceUrl + 'batch', formData, {responseType: 'text'})
      .pipe(
        tap(() => console.log('added batch')),
        catchError((error: HttpErrorResponse) => {
          console.log(`Error adding batch: ${error.message}`)
          throw error;
      }));
  }

  updateBatch(batch: Partial<Batch>, files: Map<string, File>, recalculate: boolean): Observable<string> {
    const formData: FormData = new FormData();
    for (const [key, value] of Object.entries(batch)) {
      formData.append(key, value?.toString() || "");
    }
    formData.append("recalculate", recalculate ? "true" : "false");
    if (files.get('metadata')) {
      formData.append('metadata', files.get('metadata') as File);
    }
    return this.http.put(this.serviceUrl + 'batch', formData, {responseType: 'text'})
      .pipe(
        tap(() => console.log('updated batch')),
        catchError((error: HttpErrorResponse) => {
          console.log(`Error updating batch: ${error.message}`)
          throw error;
      }));
  }

  deleteBatch(id: string): Observable<string> {
    return this.http.delete(this.serviceUrl + 'batch/' + id, {responseType: 'text'})
      .pipe(
        tap(() => console.log('deleted batch')),
        catchError((error: HttpErrorResponse) => {
          console.log(`Error deleting batch: ${error.message}`)
          throw error;
      }));
  }

  addDataset(dataset: Partial<Dataset>): Observable<string> {
    const formData: FormData = new FormData();
    for (const [key, value] of Object.entries(dataset)) {
      formData.append(key, value?.toString() || "");
    }
    return this.http.post(this.serviceUrl + 'dataset', formData, {responseType: 'text'})
      .pipe(
        tap(() => console.log('added dataset')),
        catchError((error: HttpErrorResponse) => {
          console.log(`Error adding dataset: ${error.message}`)
          throw error;
      }));
  }

  updateDataset(dataset: Partial<Dataset>): Observable<string> {
    const formData: FormData = new FormData();
    for (const [key, value] of Object.entries(dataset)) {
      formData.append(key, value?.toString() || "");
    }
    return this.http.put(this.serviceUrl + 'dataset', formData, {responseType: 'text'})
      .pipe(
        tap(() => console.log('updated dataset')),
        catchError((error: HttpErrorResponse) => {
          console.log(`Error updating dataset: ${error.message}`)
          throw error;
      }));
  }

  deleteDataset(id: string): Observable<string> {
    return this.http.delete(this.serviceUrl + 'dataset/' + id, {responseType: 'text'})
      .pipe(
        tap(() => console.log('deleted dataset')),
        catchError((error: HttpErrorResponse) => {
          console.log(`Error deleting dataset: ${error.message}`)
          throw error;
      }));
  }

  addPlatform(platform: Partial<Platform>, file: File, type: string):  Observable<string> {
    const formData: FormData = new FormData();
    for (const [key, value] of Object.entries(platform)) {
      formData.append(key, value?.toString() || "");
    }
    formData.append('type', type);
    formData.append('platformFile', file);
    return this.http.post(this.serviceUrl + 'platform', formData, {responseType: 'text'})
      .pipe(
        tap(() => console.log('added platform')),
        catchError((error: HttpErrorResponse) => {
          console.log(`Error adding platform: ${error.message}`)
          throw error;
      }));
  }

  updatePlatform(platform: Partial<Platform>):  Observable<string> {
    const formData: FormData = new FormData();
    for (const [key, value] of Object.entries(platform)) {
      formData.append(key, value?.toString() || "");
    }
    return this.http.put(this.serviceUrl + 'platform', formData, {responseType: 'text'})
      .pipe(
        tap(() => console.log('updated platform')),
        catchError((error: HttpErrorResponse) => {
          console.log(`Error updating platform: ${error.message}`)
          throw error;
      }));
  }

  deletePlatform(id: string): Observable<string> {
    return this.http.delete(this.serviceUrl + 'platform/' + id, {responseType: 'text'})
      .pipe(
        tap(() => console.log('deleted platform')),
        catchError((error: HttpErrorResponse) => {
          console.log(`Error deleting platform: ${error.message}`)
          throw error;
      }));
  }

}
