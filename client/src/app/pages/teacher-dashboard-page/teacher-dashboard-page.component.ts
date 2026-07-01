import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';

import {
  EinAbCategory,
  IcalCandidate,
  IcalCandidateListResponse,
  SlotStatus,
  SlotResponse,
  TeacherEinAbListResponse,
  TeacherEinAbResponse,
  TeacherResponse,
  TeacherService,
  TeacherSelfResponse,
  UpdateTeacherMeRequest,
  UpsertEinAbRequest
} from '../../api';
import { resolveApiError } from '../../core/api-error';
import { I18nService } from '../../core/i18n.service';
import { ZurichDateTimePipe } from '../../core/zurich-date-time.pipe';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { CheckboxModule } from 'primeng/checkbox';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DatePickerModule } from 'primeng/datepicker';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { SelectModule } from 'primeng/select';
import { PaginatorModule } from 'primeng/paginator';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ConfirmationService, MessageService } from 'primeng/api';

@Component({
  selector: 'app-teacher-dashboard-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    ZurichDateTimePipe,
    CardModule,
    ButtonModule,
    CheckboxModule,
    ConfirmDialogModule,
    DatePickerModule,
    DialogModule,
    InputTextModule,
    TextareaModule,
    SelectModule,
    PaginatorModule,
    TableModule,
    TagModule
  ],
  templateUrl: './teacher-dashboard-page.component.html'
})
export class TeacherDashboardPageComponent implements OnInit {
  readonly i18n = inject(I18nService);
  readonly SlotStatus = SlotStatus;

  protected readonly teacher = signal<TeacherResponse | null>(null);
  protected readonly einAbs = signal<TeacherEinAbResponse[]>([]);
  protected readonly einAbsPage = signal<TeacherEinAbListResponse | null>(null);
  protected readonly selectedEinAb = signal<TeacherEinAbResponse | null>(null);
  protected readonly icalCandidates = signal<IcalCandidate[]>([]);
  protected readonly icalCandidatesPage = signal<IcalCandidateListResponse | null>(null);
  protected readonly profileSaveLoading = signal(false);
  protected readonly saveLoading = signal(false);
  protected readonly categoryOptions = computed(() => Object.values(EinAbCategory).map((value) => ({ value, label: this.i18n.categoryLabel(value) })));
  protected readonly slotCountOptions = [1, 2, 3].map((value) => ({ value, label: String(value) }));
  protected readonly pageSize = 20;

  protected einabDialogVisible = false;
  protected readonly editingEinAb = signal<TeacherEinAbResponse | null>(null);

  protected readonly einabForm = inject(FormBuilder).nonNullable.group({
    category: [EinAbCategory.Supermarket, Validators.required],
    startDateTime: [new Date(), Validators.required],
    location: [''],
    publicLocation: ['', Validators.required],
    whatToBring: [''],
    hint: [''],
    visitFairteiler: [false],
    slotCount: [1, Validators.required],
    minimumPickupCount: [null as number | null]
  });

  protected readonly teacherProfileForm = inject(FormBuilder).nonNullable.group({
    phoneNumber: ['', [Validators.required, Validators.minLength(3)]],
    icalLink: ['']
  });

  private readonly teacherApi = inject(TeacherService);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.teacherApi.getTeacherMe().subscribe({
      next: (response) => {
        this.teacher.set(response);
        this.teacherProfileForm.reset({
          phoneNumber: response.phoneNumber,
          icalLink: response.icalLink ?? ''
        });
      },
      error: (error) => this.toastError(resolveApiError(error, this.i18n))
    });
    this.teacherApi.getTeacherEinAbs({ page: this.einAbsPage()?.page ?? 0, size: this.pageSize }).subscribe({
      next: (response) => {
        this.einAbs.set(response.einAbs);
        this.einAbsPage.set(response);
        if (this.selectedEinAb()) {
          const refreshed = response.einAbs.find((item) => item.id === this.selectedEinAb()?.id) ?? null;
          this.selectedEinAb.set(refreshed);
        }
      },
      error: (error) => this.toastError(resolveApiError(error, this.i18n))
    });
    this.teacherApi.getTeacherIcalCandidates({ page: this.icalCandidatesPage()?.page ?? 0, size: this.pageSize }).subscribe({
      next: (response) => {
        this.icalCandidates.set(response.candidates);
        this.icalCandidatesPage.set(response);
      },
      error: () => this.icalCandidates.set([])
    });
  }

  onEinAbsPageChange(event: { page?: number }): void {
    this.einAbsPage.update((current) => current ? { ...current, page: event.page ?? 0 } : current);
    this.reload();
  }

  onIcalPageChange(event: { page?: number }): void {
    this.icalCandidatesPage.update((current) => current ? { ...current, page: event.page ?? 0 } : current);
    this.reload();
  }

  saveTeacherProfile(): void {
    if (this.teacherProfileForm.invalid) {
      return;
    }
    this.profileSaveLoading.set(true);
    const formValue = this.teacherProfileForm.getRawValue();
    const updateTeacherMeRequest: UpdateTeacherMeRequest = {
      phoneNumber: formValue.phoneNumber.trim(),
      language: this.i18n.apiLanguage(),
      icalLink: formValue.icalLink?.trim() || undefined
    };
    this.teacherApi.updateTeacherMe({ updateTeacherMeRequest }).subscribe({
      next: (response: TeacherSelfResponse) => {
        this.teacher.set(response);
        this.icalCandidates.set(response.icalCandidates ?? []);
        this.icalCandidatesPage.update((current) => current ? { ...current, candidates: response.icalCandidates ?? [] } : current);
        this.teacherProfileForm.reset({ phoneNumber: response.phoneNumber, icalLink: response.icalLink ?? '' });
        this.profileSaveLoading.set(false);
      },
      error: (error) => {
        this.profileSaveLoading.set(false);
        this.toastError(resolveApiError(error, this.i18n));
      }
    });
  }

  selectEinAb(einab: TeacherEinAbResponse): void {
    this.selectedEinAb.set(einab);
  }

  openCreate(): void {
    if (!this.requireActiveTeacher()) {
      return;
    }
    this.editingEinAb.set(null);
    this.einabForm.reset({
      category: EinAbCategory.Supermarket,
      startDateTime: new Date(),
      location: '',
      publicLocation: '',
      whatToBring: '',
      hint: '',
      visitFairteiler: false,
      slotCount: 1,
      minimumPickupCount: null
    });
    this.einabDialogVisible = true;
  }

  openCreateFromCandidate(candidate: IcalCandidate): void {
    if (!this.requireActiveTeacher()) {
      return;
    }
    this.editingEinAb.set(null);
    this.einabForm.reset({
      category: EinAbCategory.Supermarket,
      startDateTime: new Date(candidate.startDateTime),
      location: candidate.location ?? '',
      publicLocation: '',
      whatToBring: '',
      hint: '',
      visitFairteiler: false,
      slotCount: 1,
      minimumPickupCount: null
    });
    this.einabDialogVisible = true;
  }

  openEdit(einab: TeacherEinAbResponse): void {
    if (!this.requireActiveTeacher()) {
      return;
    }
    this.editingEinAb.set(einab);
    this.einabForm.reset({
      category: einab.category,
      startDateTime: new Date(einab.startDateTime),
      location: einab.location ?? '',
      publicLocation: einab.publicLocation ?? '',
      whatToBring: einab.whatToBring ?? '',
      hint: einab.hint ?? '',
      visitFairteiler: einab.visitFairteiler,
      slotCount: einab.slotCount,
      minimumPickupCount: einab.minimumPickupCount ?? null
    });
    this.einabDialogVisible = true;
  }

  saveEinAb(): void {
    if (this.einabForm.invalid) {
      return;
    }
    if (!this.requireActiveTeacher()) {
      return;
    }
    this.saveLoading.set(true);
    const formValue = this.einabForm.getRawValue();
    const upsertEinAbRequest: UpsertEinAbRequest = {
      category: formValue.category,
      startDateTime: formValue.startDateTime.toISOString(),
      location: formValue.location?.trim() || undefined,
      publicLocation: formValue.publicLocation.trim(),
      whatToBring: formValue.whatToBring?.trim() || undefined,
      hint: formValue.hint?.trim() || undefined,
      visitFairteiler: formValue.visitFairteiler,
      slotCount: formValue.slotCount,
      minimumPickupCount: formValue.minimumPickupCount ?? undefined
    };

    const request$ = this.editingEinAb()
      ? this.teacherApi.updateTeacherEinAb({ einAbId: this.editingEinAb()!.id, upsertEinAbRequest })
      : this.teacherApi.createTeacherEinAb({ upsertEinAbRequest });

    request$.pipe(finalize(() => this.saveLoading.set(false))).subscribe({
      next: () => {
        this.einabDialogVisible = false;
        this.reload();
      },
      error: (error) => this.toastError(resolveApiError(error, this.i18n))
    });
  }

  confirmDelete(einab: TeacherEinAbResponse): void {
    this.confirmationService.confirm({
      message: this.i18n.t('confirm.deleteEinab'),
      accept: () => {
        this.teacherApi.deleteTeacherEinAb({ einAbId: einab.id }).subscribe({
          next: () => this.reload(),
          error: (error) => this.toastError(resolveApiError(error, this.i18n))
        });
      }
    });
  }

  cancelSlotBooking(slot: SlotResponse): void {
    this.confirmationService.confirm({
      message: this.i18n.t('confirm.cancelTeacherBooking'),
      accept: () => {
        this.teacherApi.cancelTeacherSlotBooking({ slotId: slot.id }).subscribe({
          next: () => this.reload(),
          error: (error) => this.toastError(resolveApiError(error, this.i18n))
        });
      }
    });
  }

  private toastError(detail: string): void {
    this.messageService.add({ severity: 'error', summary: this.i18n.t('common.error'), detail });
  }

  private requireActiveTeacher(): boolean {
    if (this.teacher()?.active) {
      return true;
    }
    this.toastError(this.i18n.t('teacher.inactiveHint'));
    return false;
  }
}
