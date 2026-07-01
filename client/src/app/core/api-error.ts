import { HttpErrorResponse } from '@angular/common/http';

import { ErrorResponse } from '../api';
import { I18nService } from './i18n.service';

export function resolveApiError(error: unknown, i18n?: I18nService): string {
  const httpError = error as HttpErrorResponse;
  const apiError = httpError?.error as ErrorResponse | undefined;
  if (apiError?.code && i18n) {
    return i18n.errorLabel(apiError.code);
  }
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
