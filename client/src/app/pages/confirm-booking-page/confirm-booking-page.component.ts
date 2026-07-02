import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { BookingDetailResponse, PublicService } from '../../api';
import { resolveApiError } from '../../core/api-error';
import { I18nService } from '../../core/i18n.service';
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

  private readonly route = inject(ActivatedRoute);
  private readonly publicApi = inject(PublicService);

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) {
      this.loading.set(false);
      this.error.set(this.i18n.t('auth.missingToken'));
      return;
    }
    this.publicApi.confirmBooking({ verifyTokenRequest: { token } }).subscribe({
      next: (booking) => {
        this.booking.set(booking);
        this.loading.set(false);
      },
      error: (error) => {
        this.error.set(resolveApiError(error, this.i18n));
        this.loading.set(false);
      }
    });
  }
}
