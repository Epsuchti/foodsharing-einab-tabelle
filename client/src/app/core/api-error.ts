import { HttpErrorResponse } from '@angular/common/http';

import { ErrorResponse } from '../api';
import { I18nService } from './i18n.service';

export function resolveApiError(error: unknown, i18n?: I18nService): string {
  const httpError = error instanceof HttpErrorResponse ? error : null;
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
  if (httpError) {
    if (httpError.status === 0 && i18n) {
      return i18n.t('error.NETWORK_ERROR');
    }
    if (i18n) {
      return i18n.t('error.UNEXPECTED_ERROR');
    }
  }
  if (error instanceof Error && error.message) {
    return error.message;
  }
  return i18n?.t('error.UNEXPECTED_ERROR') ?? 'Unexpected error';
}
