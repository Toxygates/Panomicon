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
    const formData: FormData = new FormData();
    formData.append("id", id);
    return this.http.delete(this.serviceUrl + 'dataset/' + id, {responseType: 'text'})
      .pipe(
        tap(() => console.log('deleted dataset')),
        catchError((error: HttpErrorResponse) => {
          console.log(`Error deleting dataset: ${error.message}`)
          throw error;
      }));
  }

}
