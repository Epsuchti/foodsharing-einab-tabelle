import { inject } from '@angular/core';
import { CanActivateFn } from '@angular/router';

import { BezirkContextService } from './bezirk-context.service';

export const bezirkGuard: CanActivateFn = (route) =>
  inject(BezirkContextService).selectBezirk(route.paramMap.get('bezirkSlug'));
