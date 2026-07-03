import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs';
import { catchError, of } from 'rxjs';

import {
  AvailableSlotResponse,
  AvailableSlotListResponse,
  BookSlotRequest,
  BookingDetailResponse,
  BookingUserResponse,
  EinAbCategory,
  PublicService,
  UserRole,
  UserService
} from '../../api';
import { resolveApiError } from '../../core/api-error';
import { I18nService } from '../../core/i18n.service';
import { SessionService } from '../../core/session.service';
import { ZurichDateTimePipe } from '../../core/zurich-date-time.pipe';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { CheckboxModule } from 'primeng/checkbox';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { SelectModule } from 'primeng/select';
import { PaginatorModule } from 'primeng/paginator';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';
import { ConfirmationService, MessageService } from 'primeng/api';

const FOODSHARING_BASE_URL = 'https://foodsharing.network';

@Component({
  selector: 'app-public-slots-page',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    ZurichDateTimePipe,
    TableModule,
    CardModule,
    ButtonModule,
    DialogModule,
    InputTextModule,
    CheckboxModule,
    ConfirmDialogModule,
    SelectModule,
    PaginatorModule,
    TagModule,
    ProgressSpinnerModule,
    ToastModule
  ],
  templateUrl: './public-slots-page.component.html',
  styleUrl: './public-slots-page.component.scss'
})
export class PublicSlotsPageComponent implements OnInit {
  readonly i18n = inject(I18nService);

  protected readonly categoryOptions = computed(() => Object.values(EinAbCategory).map((value) => ({
    value,
    label: this.i18n.categoryLabel(value)
  })));
  protected readonly slots = signal<AvailableSlotResponse[]>([]);
  protected readonly slotsPage = signal<AvailableSlotListResponse | null>(null);
  protected readonly loading = signal(false);
  protected readonly bookingLoading = signal(false);
  protected readonly bookingIdentityLocked = signal(false);
  protected readonly bookingProfile = signal<BookingUserResponse | null>(null);
  protected readonly bookingDetails = signal<BookingDetailResponse | null>(null);
  protected bookingVisible = false;
  protected bookingSuccessVisible = false;
  protected selectedSlot: AvailableSlotResponse | null = null;
  protected search = '';
  protected fairteilerOnly = false;
  protected selectedCategory?: EinAbCategory;
  protected readonly pageSize = 20;

  protected readonly bookingForm = inject(FormBuilder).nonNullable.group({
    foodsharingId: ['', [Validators.required, Validators.pattern('^\\d+$')]]
  });

  private readonly publicApi = inject(PublicService);
  private readonly userApi = inject(UserService);
  private readonly sessionService = inject(SessionService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly messageService = inject(MessageService);

  ngOnInit(): void {
    this.loadSlots();
    this.loadBookingProfile();
  }

  loadSlots(): void {
    this.loading.set(true);
    this.publicApi.getAvailableSlots({
      search: this.search || undefined,
      category: this.selectedCategory,
      visitFairteiler: this.fairteilerOnly ? true : undefined,
      page: this.slotsPage()?.page ?? 0,
      size: this.pageSize
    }).pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (response) => {
          this.slots.set(response.slots);
          this.slotsPage.set(response);
        },
        error: (error) => this.toastError(resolveApiError(error, this.i18n))
      });
  }

  onSlotsPageChange(event: { page?: number }): void {
    this.slotsPage.update((current) => current ? { ...current, page: event.page ?? 0 } : current);
    this.loadSlots();
  }

  openBooking(slot: AvailableSlotResponse): void {
    this.selectedSlot = slot;
    if (!this.bookingIdentityLocked()) {
      this.applyLoggedOutBookingDefaults();
    }
    this.bookingVisible = true;
  }

  confirmBooking(): void {
    if (!this.selectedSlot || this.bookingForm.invalid) {
      return;
    }
    this.bookingVisible = false;
    this.confirmationService.confirm({
      header: this.i18n.t('book.guestConfirmTitle'),
      message: this.i18n.t('book.guestConfirmMessage'),
      acceptLabel: this.i18n.t('common.yes'),
      rejectLabel: this.i18n.t('common.no'),
      accept: () => {
        this.confirmationService.close();
        window.open(FOODSHARING_BASE_URL, '_blank', 'noopener');
        this.performBooking();
      },
      reject: () => {
        this.confirmationService.close();
        this.bookingVisible = true;
      }
    });
  }

  private performBooking(): void {
    if (!this.selectedSlot) {
      return;
    }
    this.bookingLoading.set(true);
    const bookSlotRequest: BookSlotRequest = {
      foodsharingId: this.bookingForm.getRawValue().foodsharingId,
      language: this.i18n.apiLanguage()
    };
    this.publicApi.bookSlot({
      slotId: this.selectedSlot.slotId,
      bookSlotRequest
    }).pipe(finalize(() => this.bookingLoading.set(false)))
      .subscribe({
      next: (response) => {
          this.messageService.add({ severity: 'success', summary: this.i18n.t('book.confirmationSent') });
          this.bookingVisible = false;
          this.bookingForm.reset();
          this.loadSlots();
        },
        error: (error) => this.toastError(resolveApiError(error, this.i18n))
      });
  }

  private toastError(detail: string): void {
    this.messageService.add({ severity: 'error', summary: this.i18n.t('common.error'), detail });
  }

  protected displayPublicLocation(slot: AvailableSlotResponse | null | undefined): string {
    return slot?.publicLocation ?? slot?.location ?? '-';
  }

  protected displayBookingLocation(booking: BookingDetailResponse | null | undefined): string {
    return booking?.location ?? '-';
  }

  protected closeBookingSuccess(): void {
    this.bookingSuccessVisible = false;
    this.bookingDetails.set(null);
  }

  private loadBookingProfile(): void {
    if (!this.sessionService.isAuthenticated() || !this.sessionService.hasRole(UserRole.User)) {
      return;
    }

    this.userApi.getMyProfile().pipe(
      catchError((error: unknown) => {
        if (error instanceof HttpErrorResponse && error.status === 404) {
          this.applyLoggedOutBookingDefaults();
          return of(null);
        }
        this.toastError(resolveApiError(error, this.i18n));
        return of(null);
      })
    ).subscribe({
      next: (profile) => {
        if (!profile) {
          return;
        }
        this.bookingProfile.set(profile);
        this.bookingIdentityLocked.set(true);
        this.bookingForm.patchValue({
          foodsharingId: profile.foodsharingId
        });
        this.bookingForm.controls.foodsharingId.disable({ emitEvent: false });
      }
    });
  }

  private applyLoggedOutBookingDefaults(): void {
    this.bookingProfile.set(null);
    this.bookingIdentityLocked.set(false);
    this.bookingForm.controls.foodsharingId.enable({ emitEvent: false });
    this.bookingForm.patchValue({ foodsharingId: this.sessionService.session()?.foodsharingId ?? '' });
  }
}
