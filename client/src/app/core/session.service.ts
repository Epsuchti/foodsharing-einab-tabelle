import { Injectable, computed, signal } from '@angular/core';
import { Router } from '@angular/router';

import { AuthResponse, UserRole } from '../api';

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

  roles(): UserRole[] {
    return this.session()?.roles ?? [];
  }

  hasRole(role: UserRole): boolean {
    return this.roles().includes(role);
  }

  primaryRoute(): string {
    if (this.hasRole(UserRole.Admin)) {
      return '/admin';
    }
    if (this.hasRole(UserRole.Teacher)) {
      return '/teacher';
    }
    if (this.hasRole(UserRole.User)) {
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
