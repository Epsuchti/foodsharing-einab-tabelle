import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';

import {
  AdminEinAbListResponse,
  AdminBookingUserPageResponse,
  AdminBookingUserResponse,
  BookingListResponse,
  BookingDetailResponse,
  EinAbResponse,
  TeacherListResponse,
  TeacherResponse,
  AdminService
} from '../../api';
import { resolveApiError } from '../../core/api-error';
import { I18nService } from '../../core/i18n.service';
import { ZurichDateTimePipe } from '../../core/zurich-date-time.pipe';
import { AccordionModule } from 'primeng/accordion';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { CheckboxModule } from 'primeng/checkbox';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { PaginatorModule } from 'primeng/paginator';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { InputTextModule } from 'primeng/inputtext';
import { InputNumberModule } from 'primeng/inputnumber';
import { ConfirmationService, MessageService } from 'primeng/api';

@Component({
  selector: 'app-admin-dashboard-page',
  standalone: true,
  imports: [
    CommonModule,
    ZurichDateTimePipe,
    FormsModule,
    AccordionModule,
    CardModule,
    ButtonModule,
    CheckboxModule,
    ConfirmDialogModule,
    PaginatorModule,
    TableModule,
    TagModule,
    InputTextModule,
    InputNumberModule
  ],
  templateUrl: './admin-dashboard-page.component.html'
})
export class AdminDashboardPageComponent implements OnInit {
  readonly i18n = inject(I18nService);

  protected readonly teachers = signal<TeacherResponse[]>([]);
  protected readonly teachersPage = signal<TeacherListResponse | null>(null);
  protected readonly einAbs = signal<EinAbResponse[]>([]);
  protected readonly einAbsPage = signal<AdminEinAbListResponse | null>(null);
  protected readonly bookings = signal<BookingDetailResponse[]>([]);
  protected readonly bookingsPage = signal<BookingListResponse | null>(null);
  protected readonly usersPage = signal<AdminBookingUserPageResponse | null>(null);
  protected readonly onlyThreePickups = signal(false);
  protected readonly usersLoading = signal(true);
  protected readonly foodsharingStatus = signal<FoodsharingConnectionStatus | null>(null);
  protected readonly foodsharingStores = signal<FoodsharingStoreAutomation[]>([]);
  protected readonly foodsharingAudit = signal<FoodsharingAutomationAudit[]>([]);
  protected readonly foodsharingFuturePickupUsers = signal<FoodsharingFuturePickupUser[]>([]);
  protected readonly foodsharingEmail = signal('');
  protected foodsharingPassword = '';
  protected readonly foodsharingRunResult = signal<FoodsharingRunResult | null>(null);

  protected readonly pageSize = 20;

  private readonly adminApi = inject(AdminService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly messageService = inject(MessageService);
  private readonly http = inject(HttpClient);

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.adminApi.getAdminTeachers({ page: this.teachersPage()?.page ?? 0, size: this.pageSize }).subscribe({
      next: (response) => {
        this.teachers.set(response.teachers);
        this.teachersPage.set(response);
      },
      error: (error) => this.toastError(resolveApiError(error, this.i18n))
    });
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
    this.loadFoodsharingAutomation();
  }

  toggleTeacher(teacher: TeacherResponse, active: boolean): void {
    this.confirmationService.confirm({
      message: this.i18n.t(active ? 'confirm.enableTeacher' : 'confirm.disableTeacher'),
      accept: () => {
        this.confirmationService.close();
        const request$ = active
          ? this.adminApi.enableAdminTeacher({ teacherId: teacher.id })
          : this.adminApi.disableAdminTeacher({ teacherId: teacher.id });
        request$.subscribe({
          next: () => this.reload(),
          error: (error) => this.toastError(resolveApiError(error, this.i18n))
        });
      }
    });
  }

  toggleAdmin(teacher: TeacherResponse, admin: boolean): void {
    this.confirmationService.confirm({
      message: this.i18n.t(admin ? 'confirm.grantAdmin' : 'confirm.revokeAdmin'),
      accept: () => {
        this.confirmationService.close();
        const request$ = admin
          ? this.adminApi.grantAdminTeacher({ teacherId: teacher.id })
          : this.adminApi.revokeAdminTeacher({ teacherId: teacher.id });
        request$.subscribe({
          next: () => this.reload(),
          error: (error) => this.toastError(resolveApiError(error, this.i18n))
        });
      }
    });
  }

  loadUsersPage(page: number): void {
    this.usersLoading.set(true);
    this.adminApi.getAdminUsers({
      page,
      size: this.pageSize,
      threePickupsOnly: this.onlyThreePickups()
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

  onTeachersPageChange(event: { page?: number }): void {
    this.teachersPage.update((current) => current ? { ...current, page: event.page ?? 0 } : current);
    this.reload();
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

  userPanels(): AdminBookingUserResponse[] {
    return this.usersPage()?.users ?? [];
  }

  trackByUserId(_: number, user: AdminBookingUserResponse): string {
    return user.user.id;
  }


  connectFoodsharing(): void {
    this.http.post<FoodsharingConnectionStatus>('/api/admin/foodsharing/connect', { email: this.foodsharingEmail(), password: this.foodsharingPassword }).subscribe({
      next: (status) => { this.foodsharingStatus.set(status); this.foodsharingPassword = ''; this.loadFoodsharingAutomation(); },
      error: (error) => this.toastError(resolveApiError(error, this.i18n))
    });
  }

  disconnectFoodsharing(): void {
    this.http.delete<void>('/api/admin/foodsharing/connect').subscribe({
      next: () => { this.foodsharingStatus.set({ connected: false }); this.foodsharingStores.set([]); this.foodsharingFuturePickupUsers.set([]); },
      error: (error) => this.toastError(resolveApiError(error, this.i18n))
    });
  }

  saveFoodsharingStore(store: FoodsharingStoreAutomation): void {
    this.http.put<FoodsharingStoreAutomation>(`/api/admin/foodsharing/stores/${store.storeId}/automation`, store).subscribe({
      next: () => this.loadFoodsharingAutomation(),
      error: (error) => this.toastError(resolveApiError(error, this.i18n))
    });
  }

  runFoodsharingDryRun(): void {
    this.http.post<FoodsharingRunResult>('/api/admin/foodsharing/automation/run', { dryRun: true }).subscribe({
      next: (result) => { this.foodsharingRunResult.set(result); this.loadFoodsharingAudit(); },
      error: (error) => this.toastError(resolveApiError(error, this.i18n))
    });
  }

  private loadFoodsharingAutomation(): void {
    this.http.get<FoodsharingConnectionStatus>('/api/admin/foodsharing/status').subscribe({
      next: (status) => {
        this.foodsharingStatus.set(status);
        if (status.connected) {
          this.http.get<FoodsharingStoreAutomation[]>('/api/admin/foodsharing/stores').subscribe({ next: (stores) => this.foodsharingStores.set(stores), error: () => undefined });
          this.loadFoodsharingFuturePickupUsers();
          this.loadFoodsharingAudit();
        }
      },
      error: () => undefined
    });
  }

  private loadFoodsharingFuturePickupUsers(): void {
    this.http.get<FoodsharingFuturePickupUser[]>('/api/admin/foodsharing/future-pickup-users').subscribe({
      next: (users) => this.foodsharingFuturePickupUsers.set(users),
      error: () => undefined
    });
  }

  private loadFoodsharingAudit(): void {
    this.http.get<FoodsharingAutomationAudit[]>('/api/admin/foodsharing/automation/audit').subscribe({ next: (audit) => this.foodsharingAudit.set(audit), error: () => undefined });
  }

  private toastError(detail: string): void {
    this.messageService.add({ severity: 'error', summary: this.i18n.t('common.error'), detail });
  }
}


interface FoodsharingConnectionStatus { connected: boolean; email?: string; foodsharingUserId?: string; authenticatedAt?: string; }
interface FoodsharingStoreAutomation { storeId: number; storeName: string; enabled: boolean; gapRuleEnabled: boolean; minimumGapDays: number; cleaningRuleEnabled: boolean; }
interface FoodsharingAutomationAudit { storeId: number; foodsharingUserId: string; pickupDate: string; dryRun: boolean; decision: string; reasons: string; error?: string; createdAt: string; }
interface FoodsharingFuturePickupUser { foodsharingUserId: string; name: string; futurePickupCount: number; futurePickups: FoodsharingFuturePickup[]; }
interface FoodsharingFuturePickup { storeId: number; storeName: string; pickupDate: string; confirmed: boolean; }
interface FoodsharingRunResult { evaluated: number; confirmed: number; declined: number; failed: number; dryRun: boolean; messages: string[]; }
