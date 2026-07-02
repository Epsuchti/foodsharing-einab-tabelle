import { HttpErrorResponse, HttpEvent, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, Observable, throwError } from 'rxjs';

import { SessionService } from './session.service';

export const authInterceptor: HttpInterceptorFn = (request, next): Observable<HttpEvent<unknown>> => {
  const sessionService = inject(SessionService);
  const token = sessionService.token();
  if (!token) {
    return next(request).pipe(handleUnauthorized(sessionService)) as Observable<HttpEvent<unknown>>;
  }
  return next(request.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`
    }
  })).pipe(handleUnauthorized(sessionService)) as Observable<HttpEvent<unknown>>;
};

function handleUnauthorized(sessionService: SessionService) {
  return catchError((error: unknown): Observable<HttpEvent<unknown>> => {
    if (error instanceof HttpErrorResponse && error.status === 401 && sessionService.isAuthenticated()) {
      sessionService.clearSession();
    }
    return throwError(() => error);
  });
}
