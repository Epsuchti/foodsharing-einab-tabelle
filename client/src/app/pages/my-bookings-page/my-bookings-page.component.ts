import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';

import { BookingDetailResponse, BookingListResponse, SlotStatus, UserService } from '../../api';
import { resolveApiError } from '../../core/api-error';
import { I18nService } from '../../core/i18n.service';
import { ZurichDateTimePipe } from '../../core/zurich-date-time.pipe';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { PaginatorModule } from 'primeng/paginator';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ConfirmationService, MessageService } from 'primeng/api';

@Component({
  selector: 'app-my-bookings-page',
  standalone: true,
  imports: [CommonModule, ZurichDateTimePipe, CardModule, ButtonModule, PaginatorModule, TableModule, TagModule],
  templateUrl: './my-bookings-page.component.html'
})
export class MyBookingsPageComponent implements OnInit {
  readonly i18n = inject(I18nService);
  readonly SlotStatus = SlotStatus;
  protected readonly bookings = signal<BookingDetailResponse[]>([]);
  protected readonly bookingsPage = signal<BookingListResponse | null>(null);
  protected readonly pageSize = 20;

  private readonly userApi = inject(UserService);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);

  ngOnInit(): void {
    this.reload();
  }

  confirmCancel(slotId: string): void {
    this.confirmationService.confirm({
      message: this.i18n.t('confirm.cancelBooking'),
      accept: () => {
        this.userApi.cancelMyBooking({ slotId }).subscribe({
          next: () => this.reload(),
          error: (error) =>
            this.messageService.add({ severity: 'error', summary: resolveApiError(error, this.i18n) })
        });
      }
    });
  }

  onPageChange(event: { page?: number }): void {
    this.loadPage(event.page ?? 0);
  }

  private reload(): void {
    this.loadPage(this.bookingsPage()?.page ?? 0);
  }

  private loadPage(page: number): void {
    this.userApi.getMyBookings({ page, size: this.pageSize }).subscribe({
      next: (response) => {
        this.bookings.set(response.bookings);
        this.bookingsPage.set(response);
      },
      error: (error) => this.messageService.add({ severity: 'error', summary: resolveApiError(error, this.i18n) })
    });
  }
}
