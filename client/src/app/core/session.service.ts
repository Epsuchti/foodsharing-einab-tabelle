import { Injectable, computed, signal } from '@angular/core';
import { Router } from '@angular/router';

import { AuthResponse, UserPermission } from '../api';

type SessionState = AuthResponse | null;

@Injectable({ providedIn: 'root' })
export class SessionService {
  private readonly storageKey = 'foodsharing.session';
  readonly session = signal<SessionState>(this.loadSession());
  readonly isAuthenticated = computed(() => {
    const session = this.session();
    return Boolean(session && new Date(session.expiresAt).getTime() > Date.now());
  });

  constructor(private readonly router: Router) {}

  setSession(session: AuthResponse): void {
    this.session.set(session);
    localStorage.setItem(this.storageKey, JSON.stringify(session));
  }

  clearSession(): void {
    this.session.set(null);
    localStorage.removeItem(this.storageKey);
    this.router.navigateByUrl('/');
  }

  token(): string | null {
    return this.isAuthenticated() ? this.session()?.authToken ?? null : null;
  }


  permissions(): UserPermission[] {
    return this.session()?.permissions ?? [];
  }

  hasPermission(permission: UserPermission): boolean {
    return this.permissions().includes(permission);
  }

  primaryRoute(): string {
    if (this.hasPermission(UserPermission.CanManageUsers)) {
      return '/admin';
    }
    if (
      this.hasPermission(UserPermission.CanSeeUserPickupCountGrouping)
      || this.hasPermission(UserPermission.CanUseAutomationSlotApproval)
      || this.hasPermission(UserPermission.CanSeeAllAutomationDecisions)
    ) {
      return '/admin/foodsharing-automation';
    }
    if (this.hasPermission(UserPermission.CanGiveEinAbs)) {
      return '/teacher';
    }
    if (this.isAuthenticated()) {
      return '/my-bookings';
    }
    return '/';
  }

  foodsharingId(): string | null {
    return this.session()?.foodsharingId ?? null;
  }

  private loadSession(): SessionState {
    const raw = localStorage.getItem(this.storageKey);
    if (!raw) {
      return null;
    }
    try {
      return JSON.parse(raw) as AuthResponse;
    } catch {
      localStorage.removeItem(this.storageKey);
      return null;
    }
  }
}
