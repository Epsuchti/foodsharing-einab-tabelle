import { CommonModule, DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';

import {
  AvailableSlotResponse,
  BookSlotRequest,
  EinAbCategory,
  NotificationSubscriptionRequest,
  PublicService
} from '../../api';
import { resolveApiError } from '../../core/api-error';
import { I18nService } from '../../core/i18n.service';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { CheckboxModule } from 'primeng/checkbox';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';

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
    SelectModule,
    TagModule,
    ProgressSpinnerModule,
    ToastModule
  ],
  templateUrl: './public-slots-page.component.html'
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
  protected bookingVisible = false;
  protected selectedSlot: AvailableSlotResponse | null = null;
  protected search = '';
  protected fairteilerOnly = false;
  protected selectedCategory?: EinAbCategory;

  protected readonly bookingForm = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    foodsharingId: ['', [Validators.required]],
    phoneNumber: ['', [Validators.required, Validators.minLength(3)]]
  });

  protected readonly subscriptionForm = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email]]
  });

  private readonly publicApi = inject(PublicService);
  private readonly messageService = inject(MessageService);

  ngOnInit(): void {
    this.loadSlots();
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
    this.bookingVisible = true;
  }

  submitBooking(): void {
    if (!this.selectedSlot || this.bookingForm.invalid) {
      return;
    }
    this.bookingLoading.set(true);
    const bookSlotRequest: BookSlotRequest = this.bookingForm.getRawValue();
    this.publicApi.bookSlot({
      slotId: this.selectedSlot.slotId,
      bookSlotRequest
    }).pipe(finalize(() => this.bookingLoading.set(false)))
      .subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: this.i18n.t('book.success') });
          this.bookingVisible = false;
          this.bookingForm.reset();
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
    this.messageService.add({ severity: 'error', summary: detail });
  }
}
