import { Injectable } from "@angular/core";
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { HandleError, HttpErrorHandler } from './http-error-handler.service';

@Injectable()
export class ModuleService {

  http: HttpClient;
  private handleError: HandleError;

  moduleListUrl = '/api/modules';
  deployApplicationUrl = "/api/deploy";
  undeployApplicationUrl = "/api/undeploy"

  constructor(http: HttpClient, httpErrorHandler: HttpErrorHandler) {
    this.http = http;
    this.handleError = httpErrorHandler.createHandleError('ModuleListService');
  }

  fetchModuleList(): Observable<any> {
    return this.http.get<any>(this.moduleListUrl)
      .pipe(
        catchError(this.handleError('fetchPaginatedProductsList', ''))
      );
  }

  deployApplication(application: string): Observable<any> {
    let payload = {
      application: application
    }
    return this.http.post<any>(this.deployApplicationUrl, payload)
      .pipe(map(response => response))
      .pipe(catchError(this.handleError('deployApplication', {status: 'error'})));
  }

  undeployApplication(application: string) {
    let payload = {
      application: application
    }
    return this.http.post<any>(this.undeployApplicationUrl, payload)
      .pipe(map(response => response))
      .pipe(catchError(this.handleError('undeployApplication', {status: 'error'})));
  }
}
