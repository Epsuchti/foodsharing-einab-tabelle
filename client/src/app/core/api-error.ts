import { HttpErrorResponse } from '@angular/common/http';

import { ErrorResponse } from '../api';

export function resolveApiError(error: unknown): string {
  const httpError = error as HttpErrorResponse;
  const apiError = httpError?.error as ErrorResponse | undefined;
  if (apiError?.details?.length) {
    return apiError.details.join(', ');
  }
  if (apiError?.message) {
    return apiError.message;
  }
  if (httpError?.message) {
    return httpError.message;
  }
  return 'Unexpected error';
}
