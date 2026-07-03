import { CommonModule } from '@angular/common';
import { HttpHeaders } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { AuthResponse, BookingDetailResponse, PublicService, UserRole } from '../../api';
import { resolveApiError } from '../../core/api-error';
import { I18nService } from '../../core/i18n.service';
import { SessionService } from '../../core/session.service';
import { ZurichDateTimePipe } from '../../core/zurich-date-time.pipe';
import { CardModule } from 'primeng/card';
import { ProgressSpinnerModule } from 'primeng/progressspinner';

@Component({
  selector: 'app-confirm-booking-page',
  standalone: true,
  imports: [CommonModule, CardModule, ProgressSpinnerModule, ZurichDateTimePipe],
  templateUrl: './confirm-booking-page.component.html'
})
export class ConfirmBookingPageComponent implements OnInit {
  readonly i18n = inject(I18nService);
  protected readonly loading = signal(true);
  protected readonly error = signal('');
  protected readonly booking = signal<BookingDetailResponse | null>(null);
  protected readonly sessionService = inject(SessionService);

  private readonly route = inject(ActivatedRoute);
  private readonly publicApi = inject(PublicService);

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) {
      this.loading.set(false);
      this.error.set(this.i18n.t('auth.missingToken'));
      return;
    }
    this.publicApi.confirmBooking({ verifyTokenRequest: { token } }, 'response').subscribe({
      next: (response) => {
        if (response.body) {
          this.booking.set(response.body);
        }
        this.applyAuthSession(response.headers);
        this.loading.set(false);
      },
      error: (error) => {
        this.error.set(resolveApiError(error, this.i18n));
        this.loading.set(false);
      }
    });
  }

  private applyAuthSession(headers: HttpHeaders): void {
    const authToken = headers.get('X-Auth-Token');
    const expiresAt = headers.get('X-Auth-Expires-At');
    const foodsharingId = headers.get('X-Auth-Foodsharing-Id');
    const roles = headers.get('X-Auth-Roles');
    if (!authToken || !expiresAt || !foodsharingId || !roles) {
      return;
    }
    const displayName = headers.get('X-Auth-Display-Name') || undefined;
    const authResponse: AuthResponse = {
      authToken,
      expiresAt,
      email: undefined,
      foodsharingId,
      displayName,
      roles: roles.split(',').filter(Boolean).map((role) => role.trim() as UserRole)
    };
    this.sessionService.setSession(authResponse);
  }
}
