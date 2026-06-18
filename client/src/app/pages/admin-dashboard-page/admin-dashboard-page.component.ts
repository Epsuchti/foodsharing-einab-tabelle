import { CommonModule, DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';

import {
  BookingDetailResponse,
  BookingUserResponse,
  EinAbResponse,
  TeacherResponse,
  AdminService
} from '../../api';
import { resolveApiError } from '../../core/api-error';
import { I18nService } from '../../core/i18n.service';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ConfirmationService, MessageService } from 'primeng/api';

@Component({
  selector: 'app-admin-dashboard-page',
  standalone: true,
  imports: [CommonModule, DatePipe, CardModule, ButtonModule, ConfirmDialogModule, TableModule, TagModule],
  templateUrl: './admin-dashboard-page.component.html'
})
export class AdminDashboardPageComponent implements OnInit {
  readonly i18n = inject(I18nService);

  protected readonly teachers = signal<TeacherResponse[]>([]);
  protected readonly einAbs = signal<EinAbResponse[]>([]);
  protected readonly bookings = signal<BookingDetailResponse[]>([]);
  protected readonly users = signal<BookingUserResponse[]>([]);

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
    this.adminApi.getAdminUsers().subscribe({
      next: (response) => this.users.set(response.users),
      error: (error) => this.toastError(resolveApiError(error))
    });
  }

  toggleTeacher(teacher: TeacherResponse, active: boolean): void {
    this.confirmationService.confirm({
      message: this.i18n.t(active ? 'confirm.enableTeacher' : 'confirm.disableTeacher'),
      accept: () => {
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

  private toastError(detail: string): void {
    this.messageService.add({ severity: 'error', summary: detail });
  }
}
