import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { UserRole } from '../api';
import { SessionService } from './session.service';

export const authGuard: CanActivateFn = (route) => {
  const sessionService = inject(SessionService);
  const router = inject(Router);
  if (!sessionService.isAuthenticated()) {
    return router.createUrlTree(['/login']);
  }

  const requiredRoles = route.data['roles'] as UserRole[] | undefined;
  if (!requiredRoles || requiredRoles.some((role) => sessionService.hasRole(role))) {
    return true;
  }
  return router.createUrlTree([sessionService.primaryRoute()]);
};
