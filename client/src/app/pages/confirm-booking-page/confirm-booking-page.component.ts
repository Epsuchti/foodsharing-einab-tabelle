import { CommonModule } from '@angular/common';
import { HttpHeaders } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { AuthResponse, BezirkResponse, BookingDetailResponse, PublicService, UserPermission } from '../../api';
import { resolveApiError } from '../../core/api-error';
import { BezirkContextService } from '../../core/bezirk-context.service';
import { I18nService } from '../../core/i18n.service';
import { SessionService } from '../../core/session.service';
import { ZurichDateTimePipe } from '../../core/zurich-date-time.pipe';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { ProgressSpinnerModule } from 'primeng/progressspinner';

@Component({
  selector: 'app-confirm-booking-page',
  standalone: true,
  imports: [CommonModule, ButtonModule, CardModule, ProgressSpinnerModule, ZurichDateTimePipe],
  templateUrl: './confirm-booking-page.component.html',
  styleUrl: './confirm-booking-page.component.scss'
})
export class ConfirmBookingPageComponent implements OnInit {
  readonly i18n = inject(I18nService);
  protected readonly loading = signal(true);
  protected readonly error = signal('');
  protected readonly booking = signal<BookingDetailResponse | null>(null);
  protected readonly sessionService = inject(SessionService);

  private readonly route = inject(ActivatedRoute);
  private readonly publicApi = inject(PublicService);
  private readonly bezirkContext = inject(BezirkContextService);

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
        this.applyAuthSession(response.headers, response.body?.bezirk);
        this.loading.set(false);
      },
      error: (error) => {
        this.error.set(resolveApiError(error, this.i18n));
        this.loading.set(false);
      }
    });
  }

  private applyAuthSession(headers: HttpHeaders, bezirk?: BezirkResponse): void {
    const authToken = headers.get('X-Auth-Token');
    const expiresAt = headers.get('X-Auth-Expires-At');
    const foodsharingId = headers.get('X-Auth-Foodsharing-Id');
    const permissions = headers.get('X-Auth-Permissions');
    if (!authToken || !expiresAt || !foodsharingId || !permissions) {
      return;
    }
    const displayName = headers.get('X-Auth-Display-Name') || undefined;
    const authResponse: AuthResponse = {
      authToken,
      expiresAt,
      email: undefined,
      foodsharingId,
      displayName,
      bezirk: bezirk ?? this.bezirkContext.selectedBezirk() ?? undefined,
      permissions: permissions.split(',').filter(Boolean).map((permission) => permission.trim() as UserPermission)
    };
    this.sessionService.setSession(authResponse);
  }
}
