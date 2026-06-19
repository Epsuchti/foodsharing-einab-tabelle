import { CommonModule, DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs';
import { catchError, of } from 'rxjs';

import {
  AvailableSlotResponse,
  BookSlotRequest,
  BookingDetailResponse,
  BookingUserResponse,
  EinAbCategory,
  NotificationSubscriptionRequest,
  PublicService,
  UserRole,
  UserService
} from '../../api';
import { resolveApiError } from '../../core/api-error';
import { I18nService } from '../../core/i18n.service';
import { SessionService } from '../../core/session.service';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { CheckboxModule } from 'primeng/checkbox';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';
import { ConfirmationService, MessageService } from 'primeng/api';

@Component({
  selector: 'app-public-slots-page',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    DatePipe,
    TableModule,
    CardModule,
    ButtonModule,
    DialogModule,
    InputTextModule,
    CheckboxModule,
    ConfirmDialogModule,
    SelectModule,
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
  protected readonly loading = signal(false);
  protected readonly bookingLoading = signal(false);
  protected readonly subscribeLoading = signal(false);
  protected readonly bookingIdentityLocked = signal(false);
  protected readonly bookingProfile = signal<BookingUserResponse | null>(null);
  protected readonly bookingDetails = signal<BookingDetailResponse | null>(null);
  protected bookingVisible = false;
  protected bookingSuccessVisible = false;
  protected selectedSlot: AvailableSlotResponse | null = null;
  protected search = '';
  protected fairteilerOnly = false;
  protected selectedCategory?: EinAbCategory;

  protected readonly bookingForm = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    name: ['', [Validators.required]],
    foodsharingId: ['', [Validators.required]],
    phoneNumber: ['', [Validators.required, Validators.minLength(3)]]
  });

  protected readonly subscriptionForm = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email]]
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
      visitFairteiler: this.fairteilerOnly ? true : undefined
    }).pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (response) => this.slots.set(response.slots),
        error: (error) => this.toastError(resolveApiError(error))
      });
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
    if (!this.sessionService.isAuthenticated()) {
      this.bookingVisible = false;
      this.confirmationService.confirm({
        header: this.i18n.t('book.guestConfirmTitle'),
        message: this.i18n.t('book.guestConfirmMessage'),
        acceptLabel: this.i18n.t('common.yes'),
        rejectLabel: this.i18n.t('common.no'),
        accept: () => {
          this.confirmationService.close();
          this.performBooking();
        },
        reject: () => {
          this.confirmationService.close();
          this.bookingVisible = true;
        }
      });
      return;
    }

    this.performBooking();
  }

  private performBooking(): void {
    if (!this.selectedSlot) {
      return;
    }
    this.bookingLoading.set(true);
    const bookSlotRequest: BookSlotRequest = this.bookingForm.getRawValue();
    this.publicApi.bookSlot({
      slotId: this.selectedSlot.slotId,
      bookSlotRequest
    }).pipe(finalize(() => this.bookingLoading.set(false)))
      .subscribe({
        next: (response) => {
          this.messageService.add({ severity: 'success', summary: this.i18n.t('book.success') });
          this.bookingVisible = false;
          this.bookingForm.reset();
          this.bookingDetails.set(response);
          this.bookingSuccessVisible = true;
          this.loadSlots();
        },
        error: (error) => this.toastError(resolveApiError(error))
      });
  }

  subscribe(): void {
    if (this.subscriptionForm.invalid) {
      return;
    }
    this.subscribeLoading.set(true);
    const notificationSubscriptionRequest: NotificationSubscriptionRequest = this.subscriptionForm.getRawValue();
    this.publicApi.subscribeNotifications({ notificationSubscriptionRequest })
      .pipe(finalize(() => this.subscribeLoading.set(false)))
      .subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: this.i18n.t('subscribe.success') });
          this.subscriptionForm.reset();
        },
        error: (error) => this.toastError(resolveApiError(error))
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
        this.toastError(resolveApiError(error));
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
          name: profile.name,
          email: profile.email,
          foodsharingId: profile.foodsharingId,
          phoneNumber: profile.phoneNumber
        });
        this.bookingForm.controls.name.disable({ emitEvent: false });
        this.bookingForm.controls.email.disable({ emitEvent: false });
        this.bookingForm.controls.foodsharingId.disable({ emitEvent: false });
        this.bookingForm.controls.phoneNumber.disable({ emitEvent: false });
      }
    });
  }

  private applyLoggedOutBookingDefaults(): void {
    this.bookingProfile.set(null);
    this.bookingIdentityLocked.set(false);
    this.bookingForm.controls.name.enable({ emitEvent: false });
    this.bookingForm.controls.email.enable({ emitEvent: false });
    this.bookingForm.controls.foodsharingId.enable({ emitEvent: false });
    this.bookingForm.controls.phoneNumber.enable({ emitEvent: false });
    this.bookingForm.patchValue({
      name: this.sessionService.session()?.displayName ?? '',
      email: this.sessionService.session()?.email ?? ''
    });
  }
}
