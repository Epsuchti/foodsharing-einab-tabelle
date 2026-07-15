import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

import {
  BookingCommentResponse,
  BookingDetailResponse,
  CreateBookingCommentRequest,
  SlotStatus,
  TeacherService
} from '../../api';
import { resolveApiError } from '../../core/api-error';
import { BezirkContextService } from '../../core/bezirk-context.service';
import { I18nService } from '../../core/i18n.service';
import { ZurichDateTimePipe } from '../../core/zurich-date-time.pipe';
import { AccordionModule } from 'primeng/accordion';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { DialogModule } from 'primeng/dialog';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { TextareaModule } from 'primeng/textarea';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ConfirmationService, MessageService } from 'primeng/api';

type BookingGroup = {
  bookingUserId: string;
  name: string;
  foodsharingId: string;
  phoneNumber?: string;
  bookings: BookingDetailResponse[];
};

@Component({
  selector: 'app-teacher-bookings-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, ZurichDateTimePipe, AccordionModule, CardModule, ButtonModule, DialogModule, ConfirmDialogModule, TableModule, TagModule, TextareaModule],
  templateUrl: './teacher-bookings-page.component.html'
})
export class TeacherBookingsPageComponent implements OnInit {
  readonly i18n = inject(I18nService);
  readonly SlotStatus = SlotStatus;

  protected readonly bookings = signal<BookingDetailResponse[]>([]);
  protected readonly commentsByUserId = signal<Record<string, BookingCommentResponse[]>>({});
  protected readonly loading = signal(false);
  protected readonly commentLoading = signal(false);
  protected readonly commentDialogVisible = signal(false);
  protected readonly commentTarget = signal<BookingGroup | null>(null);
  protected readonly commentForm = inject(FormBuilder).nonNullable.group({
    comment: ['', [Validators.required, Validators.minLength(2)]]
  });

  protected readonly bookingGroups = computed<BookingGroup[]>(() => {
    const groups = new Map<string, BookingGroup>();
    for (const booking of this.bookings()) {
      const user = booking.bookingUser;
      if (!user?.id) {
        continue;
      }
      const existing = groups.get(user.id);
      if (existing) {
        existing.bookings.push(booking);
      } else {
        groups.set(user.id, {
          bookingUserId: user.id,
          name: user.name,
          foodsharingId: user.foodsharingId,
          phoneNumber: user.phoneNumber,
          bookings: [booking]
        });
      }
    }
    return Array.from(groups.values()).sort((left, right) => left.name.localeCompare(right.name));
  });

  private readonly teacherApi = inject(TeacherService);
  private readonly bezirkContext = inject(BezirkContextService);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.teacherApi.getTeacherBookings({ bezirkSlug: this.bezirkContext.currentSlug(), page: 0, size: 100 }).subscribe({
      next: (response) => {
        this.bookings.set(response.bookings);
        this.loadComments(response.bookings);
      },
      error: (error) => {
        this.loading.set(false);
        this.toastError(resolveApiError(error, this.i18n));
      }
    });
  }

  commentsForUser(userId: string): BookingCommentResponse[] {
    return this.commentsByUserId()[userId] ?? [];
  }

  openCommentDialog(group: BookingGroup): void {
    this.commentTarget.set(group);
    this.commentForm.reset({ comment: '' });
    this.commentDialogVisible.set(true);
  }

  saveComment(): void {
    if (this.commentForm.invalid || !this.commentTarget()) {
      return;
    }

    const group = this.commentTarget()!;
    const comment = this.commentForm.getRawValue().comment.trim();
    if (!comment) {
      return;
    }

    this.commentLoading.set(true);
    const request: CreateBookingCommentRequest = { comment };
    this.teacherApi.addTeacherBookingComment({
      bezirkSlug: this.bezirkContext.currentSlug(),
      bookingUserId: group.bookingUserId,
      createBookingCommentRequest: request
    }).subscribe({
      next: () => {
        this.commentDialogVisible.set(false);
        this.commentTarget.set(null);
        this.refreshComments(group.bookingUserId);
      },
      error: (error) => {
        this.commentLoading.set(false);
        this.toastError(resolveApiError(error, this.i18n));
      }
    });
  }

  cancelBooking(slotId: string): void {
    this.confirmationService.confirm({
      message: this.i18n.t('confirm.cancelTeacherBooking'),
      accept: () => {
        this.teacherApi.cancelTeacherSlotBooking({ bezirkSlug: this.bezirkContext.currentSlug(), slotId }).subscribe({
          next: () => this.reload(),
          error: (error) => this.toastError(resolveApiError(error, this.i18n))
        });
      }
    });
  }

  private loadComments(bookings: BookingDetailResponse[]): void {
    const userIds = Array.from(new Set(
      bookings
        .map((booking) => booking.bookingUser?.id)
        .filter((value): value is string => Boolean(value))
    ));

    if (!userIds.length) {
      this.commentsByUserId.set({});
      this.loading.set(false);
      return;
    }

    forkJoin(
      userIds.map((userId) =>
        this.teacherApi.getTeacherBookingComments({ bezirkSlug: this.bezirkContext.currentSlug(), bookingUserId: userId }).pipe(
          map((response) => [userId, response.comments] as const),
          catchError(() => of([userId, []] as const))
        )
      )
    ).subscribe({
      next: (entries) => {
        this.commentsByUserId.set(Object.fromEntries(entries));
        this.loading.set(false);
      },
      error: (error) => {
        this.loading.set(false);
        this.toastError(resolveApiError(error, this.i18n));
      }
    });
  }

  private refreshComments(userId: string): void {
    this.teacherApi.getTeacherBookingComments({ bezirkSlug: this.bezirkContext.currentSlug(), bookingUserId: userId }).subscribe({
      next: (response) => {
        this.commentsByUserId.update((current) => ({ ...current, [userId]: response.comments }));
        this.commentLoading.set(false);
      },
      error: (error) => {
        this.commentLoading.set(false);
        this.toastError(resolveApiError(error, this.i18n));
      }
    });
  }

  private toastError(detail: string): void {
    this.messageService.add({ severity: 'error', summary: this.i18n.t('common.error'), detail });
  }
}
