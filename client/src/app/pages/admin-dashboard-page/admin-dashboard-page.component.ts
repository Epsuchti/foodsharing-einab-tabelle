import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import {
  AdminEinAbListResponse,
  AdminBookingUserPageResponse,
  AdminBookingUserResponse,
  BookingListResponse,
  BookingDetailResponse,
  EinAbResponse,
  AdminService
} from '../../api';
import { resolveApiError } from '../../core/api-error';
import { I18nService } from '../../core/i18n.service';
import { ZurichDateTimePipe } from '../../core/zurich-date-time.pipe';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { CheckboxModule } from 'primeng/checkbox';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { PaginatorModule } from 'primeng/paginator';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ConfirmationService, MessageService } from 'primeng/api';

@Component({
  selector: 'app-admin-dashboard-page',
  standalone: true,
  imports: [
    CommonModule,
    ZurichDateTimePipe,
    FormsModule,
    CardModule,
    ButtonModule,
    CheckboxModule,
    ConfirmDialogModule,
    PaginatorModule,
    TableModule,
    TagModule
  ],
  templateUrl: './admin-dashboard-page.component.html'
})
export class AdminDashboardPageComponent implements OnInit {
  readonly i18n = inject(I18nService);

  protected readonly einAbs = signal<EinAbResponse[]>([]);
  protected readonly einAbsPage = signal<AdminEinAbListResponse | null>(null);
  protected readonly bookings = signal<BookingDetailResponse[]>([]);
  protected readonly bookingsPage = signal<BookingListResponse | null>(null);
  protected readonly usersPage = signal<AdminBookingUserPageResponse | null>(null);
  protected readonly onlyThreePickups = signal(false);
  protected readonly activeOnly = signal(true);
  protected readonly usersLoading = signal(true);
  protected readonly expandedUserIds = signal<Record<string, boolean>>({});

  protected readonly pageSize = 20;

  private readonly adminApi = inject(AdminService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly messageService = inject(MessageService);

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.adminApi.getAdminEinAbs({ page: this.einAbsPage()?.page ?? 0, size: this.pageSize }).subscribe({
      next: (response) => {
        this.einAbs.set(response.einAbs);
        this.einAbsPage.set(response);
      },
      error: (error) => this.toastError(resolveApiError(error, this.i18n))
    });
    this.adminApi.getAdminBookings({ page: this.bookingsPage()?.page ?? 0, size: this.pageSize }).subscribe({
      next: (response) => {
        this.bookings.set(response.bookings);
        this.bookingsPage.set(response);
      },
      error: (error) => this.toastError(resolveApiError(error, this.i18n))
    });
    this.loadUsersPage(this.usersPage()?.page ?? 0);
  }

  loadUsersPage(page: number): void {
    this.usersLoading.set(true);
    this.adminApi.getAdminUsers({
      page,
      size: this.pageSize,
      threePickupsOnly: this.onlyThreePickups(),
      activeOnly: this.activeOnly()
    }).subscribe({
      next: (response) => {
        this.usersPage.set(response);
        this.usersLoading.set(false);
      },
      error: (error) => {
        this.usersLoading.set(false);
        this.toastError(resolveApiError(error, this.i18n));
      }
    });
  }

  onEinAbsPageChange(event: { page?: number }): void {
    this.einAbsPage.update((current) => current ? { ...current, page: event.page ?? 0 } : current);
    this.reload();
  }

  onBookingsPageChange(event: { page?: number }): void {
    this.bookingsPage.update((current) => current ? { ...current, page: event.page ?? 0 } : current);
    this.reload();
  }

  onUsersPageChange(event: { page?: number }): void {
    this.loadUsersPage(event.page ?? 0);
  }

  setOnlyThreePickups(checked: boolean): void {
    this.onlyThreePickups.set(checked);
    this.loadUsersPage(0);
  }

  setActiveOnly(checked: boolean): void {
    this.activeOnly.set(checked);
    this.loadUsersPage(0);
  }


  saveUserPermissions(user: AdminBookingUserResponse): void {
    this.adminApi.setAdminUserPermissions({
      userId: user.user.id,
      userPermissionsRequest: {
        canGiveEinAbs: user.user.canGiveEinAbs,
        canManageUsers: user.user.canManageUsers,
        canUseAutomations: user.user.canUseAutomations,
        canSeeUserPickupCountGrouping: user.user.canSeeUserPickupCountGrouping,
        canUseAutomationSlotApproval: user.user.canUseAutomationSlotApproval,
        canUseAutomationRequestApproval: user.user.canUseAutomationRequestApproval,
        canUseAutomationOpenSlotAdvertising: user.user.canUseAutomationOpenSlotAdvertising,
        canSeeAllAutomationDecisions: user.user.canSeeAllAutomationDecisions
      }
    }).subscribe({
      next: () => this.loadUsersPage(this.usersPage()?.page ?? 0),
      error: (error) => this.toastError(resolveApiError(error, this.i18n))
    });
  }

  disableBookingUser(user: AdminBookingUserResponse): void {
    this.confirmationService.confirm({
      message: this.i18n.t('confirm.disableBookingUser'),
      accept: () => {
        this.confirmationService.close();
        this.adminApi.disableAdminBookingUser({ bookingUserId: user.user.id }).subscribe({
          next: () => this.reload(),
          error: (error) => this.toastError(resolveApiError(error, this.i18n))
        });
      }
    });
  }

  enableBookingUser(user: AdminBookingUserResponse): void {
    this.confirmationService.confirm({
      message: this.i18n.t('confirm.enableBookingUser'),
      accept: () => {
        this.confirmationService.close();
        this.adminApi.enableAdminBookingUser({ bookingUserId: user.user.id }).subscribe({
          next: () => this.reload(),
          error: (error) => this.toastError(resolveApiError(error, this.i18n))
        });
      }
    });
  }

  userPanels(): AdminBookingUserResponse[] {
    return this.usersPage()?.users ?? [];
  }

  toggleExpandedUser(user: AdminBookingUserResponse): void {
    this.expandedUserIds.update((current) => {
      const next = { ...current };
      if (next[user.user.id]) {
        delete next[user.user.id];
      } else {
        next[user.user.id] = true;
      }
      return next;
    });
  }

  isUserExpanded(user: AdminBookingUserResponse): boolean {
    return !!this.expandedUserIds()[user.user.id];
  }

  private toastError(detail: string): void {
    this.messageService.add({ severity: 'error', summary: this.i18n.t('common.error'), detail });
  }
}
