import { CommonModule, DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import {
  AdminBookingUserPageResponse,
  AdminBookingUserResponse,
  BookingDetailResponse,
  EinAbResponse,
  TeacherResponse,
  AdminService
} from '../../api';
import { resolveApiError } from '../../core/api-error';
import { I18nService } from '../../core/i18n.service';
import { AccordionModule } from 'primeng/accordion';
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
    DatePipe,
    FormsModule,
    AccordionModule,
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

  protected readonly teachers = signal<TeacherResponse[]>([]);
  protected readonly einAbs = signal<EinAbResponse[]>([]);
  protected readonly bookings = signal<BookingDetailResponse[]>([]);
  protected readonly usersPage = signal<AdminBookingUserPageResponse | null>(null);
  protected readonly onlyThreePickups = signal(false);
  protected readonly usersLoading = signal(true);

  protected readonly pageSize = 50;

  private readonly adminApi = inject(AdminService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly messageService = inject(MessageService);

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.adminApi.getAdminTeachers().subscribe({
      next: (response) => this.teachers.set(response.teachers),
      error: (error) => this.toastError(resolveApiError(error))
    });
    this.adminApi.getAdminEinAbs().subscribe({
      next: (response) => this.einAbs.set(response.einAbs),
      error: (error) => this.toastError(resolveApiError(error))
    });
    this.adminApi.getAdminBookings().subscribe({
      next: (response) => this.bookings.set(response.bookings),
      error: (error) => this.toastError(resolveApiError(error))
    });
    this.loadUsersPage(this.usersPage()?.page ?? 0);
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
          error: (error) => this.toastError(resolveApiError(error))
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
        this.toastError(resolveApiError(error));
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

  disableBookingUser(user: AdminBookingUserResponse): void {
    this.confirmationService.confirm({
      message: this.i18n.t('confirm.disableBookingUser'),
      accept: () => {
        this.confirmationService.close();
        this.adminApi.disableAdminBookingUser({ bookingUserId: user.user.id }).subscribe({
          next: () => this.reload(),
          error: (error) => this.toastError(resolveApiError(error))
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

  private toastError(detail: string): void {
    this.messageService.add({ severity: 'error', summary: this.i18n.t('common.error'), detail });
  }
}
