import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { UserPermission } from '../api';
import { BezirkContextService } from './bezirk-context.service';
import { SessionService } from './session.service';

export const authGuard: CanActivateFn = (route) => {
  const sessionService = inject(SessionService);
  const bezirkContext = inject(BezirkContextService);
  const router = inject(Router);
  if (!sessionService.isAuthenticated()) {
    return router.createUrlTree(bezirkContext.route('login'));
  }

  const requiredPermissions = route.data['permissions'] as UserPermission[] | undefined;
  if (!requiredPermissions || requiredPermissions.some((permission) => sessionService.hasPermission(permission))) {
    return true;
  }
  return router.createUrlTree([sessionService.primaryRoute()]);
};
