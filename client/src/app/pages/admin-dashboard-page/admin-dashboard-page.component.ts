import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import {
  AdminBezirkResponse,
  AdminBookingUserPageResponse,
  AdminBookingUserResponse,
  AdminService
} from '../../api';
import { resolveApiError } from '../../core/api-error';
import { BezirkContextService } from '../../core/bezirk-context.service';
import { I18nService } from '../../core/i18n.service';
import { ZurichDateTimePipe } from '../../core/zurich-date-time.pipe';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { CheckboxModule } from 'primeng/checkbox';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { InputNumberModule } from 'primeng/inputnumber';
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
    InputNumberModule,
    PaginatorModule,
    TableModule,
    TagModule
  ],
  templateUrl: './admin-dashboard-page.component.html'
})
export class AdminDashboardPageComponent implements OnInit {
  readonly i18n = inject(I18nService);

  protected readonly usersPage = signal<AdminBookingUserPageResponse | null>(null);
  protected readonly bezirkSettings = signal<AdminBezirkResponse | null>(null);
  protected readonly cleaningStoreId = signal<number | null>(null);
  protected readonly settingsSaving = signal(false);
  protected readonly onlyThreePickups = signal(false);
  protected readonly activeOnly = signal(true);
  protected readonly unassignedOnly = signal(false);
  protected readonly usersLoading = signal(true);
  protected readonly expandedUserIds = signal<Record<string, boolean>>({});

  protected readonly pageSize = 20;

  private readonly adminApi = inject(AdminService);
  private readonly bezirkContext = inject(BezirkContextService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly messageService = inject(MessageService);

  ngOnInit(): void {
    this.loadBezirkSettings();
    this.loadUsersPage(this.usersPage()?.page ?? 0);
  }

  loadUsersPage(page: number): void {
    this.usersLoading.set(true);
    this.adminApi.getAdminUsers({
      bezirkSlug: this.bezirkContext.currentSlug(),
      page,
      size: this.pageSize,
      threePickupsOnly: this.onlyThreePickups(),
      activeOnly: this.activeOnly(),
      unassigned: this.unassignedOnly()
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

  setUnassignedOnly(checked: boolean): void {
    this.unassignedOnly.set(checked);
    this.loadUsersPage(0);
  }

  saveBezirkSettings(): void {
    this.settingsSaving.set(true);
    this.adminApi.updateAdminBezirk({
      bezirkSlug: this.bezirkContext.currentSlug(),
      updateBezirkRequest: { cleaningStoreId: this.cleaningStoreId() }
    }).subscribe({
      next: (response) => {
        this.bezirkSettings.set(response);
        this.cleaningStoreId.set(response.cleaningStoreId ?? null);
        this.settingsSaving.set(false);
        this.messageService.add({ severity: 'success', summary: this.i18n.t('common.saved') });
      },
      error: (error) => {
        this.settingsSaving.set(false);
        this.toastError(resolveApiError(error, this.i18n));
      }
    });
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
          next: () => this.loadUsersPage(this.usersPage()?.page ?? 0),
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
          next: () => this.loadUsersPage(this.usersPage()?.page ?? 0),
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

  private loadBezirkSettings(): void {
    this.adminApi.getAdminBezirk({ bezirkSlug: this.bezirkContext.currentSlug() }).subscribe({
      next: (response) => {
        this.bezirkSettings.set(response);
        this.cleaningStoreId.set(response.cleaningStoreId ?? null);
      },
      error: (error) => this.toastError(resolveApiError(error, this.i18n))
    });
  }

  private toastError(detail: string): void {
    this.messageService.add({ severity: 'error', summary: this.i18n.t('common.error'), detail });
  }
}
