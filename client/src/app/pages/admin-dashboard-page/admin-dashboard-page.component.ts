import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';

import {
  AdminBezirkResponse,
  AdminBookingUserPageResponse,
  AdminBookingUserResponse,
  AdminService,
  BezirkResponse,
  BookingDetailResponse,
  BookingUserResponse,
  CreateBookingCommentRequest,
  PublicService,
  SlotStatus
} from '../../api';
import { resolveApiError } from '../../core/api-error';
import { BezirkContextService } from '../../core/bezirk-context.service';
import { I18nService } from '../../core/i18n.service';
import { ZurichDateTimePipe } from '../../core/zurich-date-time.pipe';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { CheckboxModule } from 'primeng/checkbox';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { PaginatorModule } from 'primeng/paginator';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TextareaModule } from 'primeng/textarea';
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
    DialogModule,
    InputNumberModule,
    InputTextModule,
    ReactiveFormsModule,
    PaginatorModule,
    SelectModule,
    TableModule,
    TagModule,
    TextareaModule
  ],
  templateUrl: './admin-dashboard-page.component.html',
  styleUrl: './admin-dashboard-page.component.scss'
})
export class AdminDashboardPageComponent implements OnInit {
  private static readonly ALL_BEZIRKE = '__all__';
  private static readonly UNASSIGNED_BEZIRK = '__unassigned__';

  readonly i18n = inject(I18nService);
  readonly SlotStatus = SlotStatus;

  protected readonly usersPage = signal<AdminBookingUserPageResponse | null>(null);
  protected readonly bezirkSettings = signal<AdminBezirkResponse | null>(null);
  protected readonly bezirke = signal<BezirkResponse[]>([]);
  protected readonly selectedUserBezirki = signal<Record<string, string | null>>({});
  protected readonly cleaningStoreId = signal<number | null>(null);
  protected readonly preventDuplicateTeacherBookings = signal(true);
  protected readonly preventDuplicateCategoryBookings = signal(true);
  protected readonly maxActiveBookingsPerUser = signal(3);
  protected readonly settingsSaving = signal(false);
  protected readonly onlyThreePickups = signal(false);
  protected readonly activeOnly = signal(true);
  protected readonly selectedBezirkFilter = signal('');
  protected readonly userSearch = signal('');
  protected readonly newUserFoodsharingId = signal('');
  protected readonly creatingUser = signal(false);
  protected readonly usersLoading = signal(true);
  protected readonly expandedPickupUserIds = signal<Record<string, boolean>>({});
  protected readonly commentLoading = signal(false);
  protected readonly commentDialogVisible = signal(false);
  protected readonly commentTarget = signal<BookingDetailResponse | null>(null);
  protected readonly commentForm = inject(FormBuilder).nonNullable.group({
    comment: ['', [Validators.required, Validators.minLength(2)]]
  });

  protected readonly pageSize = 20;

  private readonly adminApi = inject(AdminService);
  private readonly publicApi = inject(PublicService);
  private readonly bezirkContext = inject(BezirkContextService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly messageService = inject(MessageService);

  ngOnInit(): void {
    this.selectedBezirkFilter.set(this.bezirkContext.currentSlug());
    this.loadBezirkSettings();
    this.loadBezirke();
    this.loadUsersPage(this.usersPage()?.page ?? 0);
  }

  loadUsersPage(page: number): void {
    this.usersLoading.set(true);
    const selectedBezirkFilter = this.selectedBezirkFilter();
    const bezirkSlug = selectedBezirkFilter && selectedBezirkFilter !== AdminDashboardPageComponent.ALL_BEZIRKE && selectedBezirkFilter !== AdminDashboardPageComponent.UNASSIGNED_BEZIRK
      ? selectedBezirkFilter
      : this.bezirkContext.currentSlug();
    this.adminApi.getAdminUsers({
      bezirkSlug,
      search: this.userSearch().trim() || undefined,
      page,
      size: this.pageSize,
      threePickupsOnly: this.onlyThreePickups(),
      activeOnly: this.activeOnly(),
      unassigned: selectedBezirkFilter === AdminDashboardPageComponent.UNASSIGNED_BEZIRK,
      allBezirke: selectedBezirkFilter === AdminDashboardPageComponent.ALL_BEZIRKE
    }).subscribe({
      next: (response) => {
        this.usersPage.set(response);
        this.selectedUserBezirki.set(Object.fromEntries((response.users ?? []).map((user) => [user.user.id, user.user.bezirk?.slug ?? null])));
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

  setBezirkFilter(bezirkSlug: string): void {
    this.selectedBezirkFilter.set(bezirkSlug);
    this.loadUsersPage(0);
  }

  setUserSearch(search: string): void {
    this.userSearch.set(search);
    this.loadUsersPage(0);
  }

  createBookingUser(): void {
    const foodsharingId = this.newUserFoodsharingId().trim();
    if (!foodsharingId || this.creatingUser()) {
      return;
    }
    this.creatingUser.set(true);
    this.adminApi.createAdminBookingUser({
      createBookingUserRequest: {
        foodsharingId,
        bezirkSlug: this.bezirkContext.currentSlug()
      }
    }).subscribe({
      next: () => {
        this.newUserFoodsharingId.set('');
        this.creatingUser.set(false);
        this.messageService.add({ severity: 'success', summary: this.i18n.t('common.created') });
        this.loadUsersPage(0);
      },
      error: (error) => {
        this.creatingUser.set(false);
        this.toastError(resolveApiError(error, this.i18n));
      }
    });
  }

  saveBezirkSettings(): void {
    this.settingsSaving.set(true);
    this.adminApi.updateAdminBezirk({
      bezirkSlug: this.bezirkContext.currentSlug(),
      updateBezirkRequest: {
        cleaningStoreId: this.cleaningStoreId(),
        preventDuplicateTeacherBookings: this.preventDuplicateTeacherBookings(),
        preventDuplicateCategoryBookings: this.preventDuplicateCategoryBookings(),
        maxActiveBookingsPerUser: this.maxActiveBookingsPerUser()
      }
    }).subscribe({
      next: (response) => {
        this.bezirkSettings.set(response);
        this.cleaningStoreId.set(response.cleaningStoreId ?? null);
        this.preventDuplicateTeacherBookings.set(response.preventDuplicateTeacherBookings ?? true);
        this.preventDuplicateCategoryBookings.set(response.preventDuplicateCategoryBookings ?? true);
        this.maxActiveBookingsPerUser.set(response.maxActiveBookingsPerUser ?? 3);
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

  selectedBezirkSlug(user: AdminBookingUserResponse): string | null {
    const selectedBezirki = this.selectedUserBezirki();
    return Object.prototype.hasOwnProperty.call(selectedBezirki, user.user.id)
      ? selectedBezirki[user.user.id]
      : user.user.bezirk?.slug ?? null;
  }

  setSelectedBezirkSlug(user: AdminBookingUserResponse, bezirkSlug: string | null): void {
    const previousBezirkSlug = this.selectedBezirkSlug(user);
    if (previousBezirkSlug === bezirkSlug) {
      return;
    }
    this.selectedUserBezirki.update((current) => ({ ...current, [user.user.id]: bezirkSlug }));
    this.adminApi.updateAdminUserBezirk({
      userId: user.user.id,
      updateUserBezirkRequest: {
        bezirkSlug
      }
    }).subscribe({
      next: (updatedUser) => {
        this.patchUser(updatedUser);
        this.messageService.add({ severity: 'success', summary: this.i18n.t('common.saved') });
      },
      error: (error) => {
        this.selectedUserBezirki.update((current) => ({ ...current, [user.user.id]: previousBezirkSlug }));
        this.toastError(resolveApiError(error, this.i18n));
      }
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

  confirmBookingCount(booking: BookingDetailResponse, counted: boolean): void {
    if (!this.isPast(booking.startDateTime)) {
      return;
    }
    this.confirmationService.confirm({
      message: this.i18n.t(counted ? 'confirm.countAgain' : 'confirm.markDidNotShowUp'),
      accept: () => {
        this.adminApi.setAdminBookingCounted({
          bezirkSlug: booking.bezirk.slug,
          slotId: booking.slotId,
          adminBookingCountRequest: { counted }
        }).subscribe({
          next: () => this.loadUsersPage(this.usersPage()?.page ?? 0),
          error: (error) => this.toastError(resolveApiError(error, this.i18n))
        });
      }
    });
  }

  openCommentDialog(booking: BookingDetailResponse): void {
    if (!booking.bookingUser) {
      return;
    }
    this.commentTarget.set(booking);
    this.commentForm.reset({ comment: '' });
    this.commentLoading.set(false);
    this.commentDialogVisible.set(true);
  }

  saveComment(): void {
    const booking = this.commentTarget();
    const comment = this.commentForm.getRawValue().comment.trim();
    if (!booking?.bookingUser || !comment || this.commentForm.invalid || this.commentLoading()) {
      return;
    }

    this.commentLoading.set(true);
    const createBookingCommentRequest: CreateBookingCommentRequest = { comment };
    this.adminApi.addAdminSlotComment({
      bezirkSlug: booking.bezirk.slug,
      slotId: booking.slotId,
      createBookingCommentRequest
    }).subscribe({
      next: () => {
        this.commentLoading.set(false);
        this.commentDialogVisible.set(false);
        this.commentTarget.set(null);
        this.loadUsersPage(this.usersPage()?.page ?? 0);
      },
      error: (error) => {
        this.commentLoading.set(false);
        this.toastError(resolveApiError(error, this.i18n));
      }
    });
  }

  userPanels(): AdminBookingUserResponse[] {
    return this.usersPage()?.users ?? [];
  }

  userFilterOptions(): { label: string; value: string }[] {
    return [
      { label: this.i18n.t('admin.allBezirke'), value: AdminDashboardPageComponent.ALL_BEZIRKE },
      { label: this.i18n.t('bezirk.unassigned'), value: AdminDashboardPageComponent.UNASSIGNED_BEZIRK },
      ...this.bezirke().map((bezirk) => ({ label: `${bezirk.name} (${bezirk.slug})`, value: bezirk.slug }))
    ];
  }

  bezirkOptions(): { label: string; value: string | null }[] {
    return [
      { label: this.i18n.t('bezirk.unassigned'), value: null },
      ...this.bezirke().map((bezirk) => ({ label: `${bezirk.name} (${bezirk.slug})`, value: bezirk.slug }))
    ];
  }

  toggleUserPickups(user: AdminBookingUserResponse): void {
    this.expandedPickupUserIds.update((current) => {
      const next = { ...current };
      if (next[user.user.id]) {
        delete next[user.user.id];
      } else {
        next[user.user.id] = true;
      }
      return next;
    });
  }

  isUserPickupsExpanded(user: AdminBookingUserResponse): boolean {
    return this.expandedPickupUserIds()[user.user.id];
  }

  protected isPast(startDateTime: string): boolean {
    return new Date(startDateTime).getTime() < Date.now();
  }

  private loadBezirkSettings(): void {
    this.adminApi.getAdminBezirk({ bezirkSlug: this.bezirkContext.currentSlug() }).subscribe({
      next: (response) => {
        this.bezirkSettings.set(response);
        this.cleaningStoreId.set(response.cleaningStoreId ?? null);
        this.preventDuplicateTeacherBookings.set(response.preventDuplicateTeacherBookings ?? true);
        this.preventDuplicateCategoryBookings.set(response.preventDuplicateCategoryBookings ?? true);
        this.maxActiveBookingsPerUser.set(response.maxActiveBookingsPerUser ?? 3);
      },
      error: (error) => this.toastError(resolveApiError(error, this.i18n))
    });
  }

  private loadBezirke(): void {
    this.publicApi.getBezirke().subscribe({
      next: (response) => this.bezirke.set(response.bezirke ?? []),
      error: () => this.bezirke.set([])
    });
  }

  private patchUser(updatedUser: BookingUserResponse): void {
    this.usersPage.update((page) => {
      if (!page) {
        return page;
      }
      return {
        ...page,
        users: page.users.map((entry) => entry.user.id === updatedUser.id
          ? {
              ...entry,
              user: updatedUser
            }
          : entry)
      };
    });
    this.selectedUserBezirki.update((current) => ({ ...current, [updatedUser.id]: updatedUser.bezirk?.slug ?? null }));
  }

  private toastError(detail: string): void {
    this.messageService.add({ severity: 'error', summary: this.i18n.t('common.error'), detail });
  }
}
