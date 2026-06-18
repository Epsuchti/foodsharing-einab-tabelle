import { CommonModule, DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';

import { BookingDetailResponse, SlotStatus, UserService } from '../../api';
import { resolveApiError } from '../../core/api-error';
import { I18nService } from '../../core/i18n.service';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ConfirmationService, MessageService } from 'primeng/api';

@Component({
  selector: 'app-my-bookings-page',
  standalone: true,
  imports: [CommonModule, DatePipe, CardModule, ButtonModule, TableModule, TagModule],
  templateUrl: './my-bookings-page.component.html'
})
export class MyBookingsPageComponent implements OnInit {
  readonly i18n = inject(I18nService);
  readonly SlotStatus = SlotStatus;
  protected readonly bookings = signal<BookingDetailResponse[]>([]);

  private readonly userApi = inject(UserService);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);

  ngOnInit(): void {
    this.userApi.getMyBookings().subscribe({
      next: (response) => this.bookings.set(response.bookings),
      error: (error) => this.messageService.add({ severity: 'error', summary: resolveApiError(error) })
    });
  }

  confirmCancel(slotId: string): void {
    this.confirmationService.confirm({
      message: this.i18n.t('confirm.cancelBooking'),
      accept: () => {
        this.userApi.cancelMyBooking({ slotId }).subscribe({
          next: () => this.reload(),
          error: (error) => this.messageService.add({ severity: 'error', summary: resolveApiError(error) })
        });
      }
    });
  }

  private reload(): void {
    this.userApi.getMyBookings().subscribe({
      next: (response) => this.bookings.set(response.bookings),
      error: (error) => this.messageService.add({ severity: 'error', summary: resolveApiError(error) })
    });
  }
}
