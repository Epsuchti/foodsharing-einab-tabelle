import { CommonModule, DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';

import {
  EinAbCategory,
  IcalCandidate,
  TeacherEinAbResponse,
  TeacherResponse,
  TeacherService,
  TeacherSelfResponse,
  UpdateTeacherMeRequest,
  UpsertEinAbRequest
} from '../../api';
import { resolveApiError } from '../../core/api-error';
import { I18nService } from '../../core/i18n.service';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { CheckboxModule } from 'primeng/checkbox';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DatePickerModule } from 'primeng/datepicker';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ConfirmationService, MessageService } from 'primeng/api';

@Component({
  selector: 'app-teacher-dashboard-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    DatePipe,
    CardModule,
    ButtonModule,
    CheckboxModule,
    ConfirmDialogModule,
    DatePickerModule,
    DialogModule,
    InputTextModule,
    TextareaModule,
    SelectModule,
    TableModule,
    TagModule
  ],
  templateUrl: './teacher-dashboard-page.component.html'
})
export class TeacherDashboardPageComponent implements OnInit {
  readonly i18n = inject(I18nService);

  protected readonly teacher = signal<TeacherResponse | null>(null);
  protected readonly einAbs = signal<TeacherEinAbResponse[]>([]);
  protected readonly selectedEinAb = signal<TeacherEinAbResponse | null>(null);
  protected readonly icalCandidates = signal<IcalCandidate[]>([]);
  protected readonly profileSaveLoading = signal(false);
  protected readonly saveLoading = signal(false);
  protected readonly categoryOptions = computed(() => Object.values(EinAbCategory).map((value) => ({ value, label: this.i18n.categoryLabel(value) })));
  protected readonly slotCountOptions = [1, 2, 3].map((value) => ({ value, label: String(value) }));

  protected einabDialogVisible = false;
  protected readonly editingEinAb = signal<TeacherEinAbResponse | null>(null);

  protected readonly einabForm = inject(FormBuilder).nonNullable.group({
    category: [EinAbCategory.Supermarket, Validators.required],
    startDateTime: [new Date(), Validators.required],
    location: [''],
    publicLocation: [''],
    whatToBring: [''],
    visitFairteiler: [false],
    slotCount: [1, Validators.required]
  });

  protected readonly teacherProfileForm = inject(FormBuilder).nonNullable.group({
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
          icalLink: response.icalLink ?? ''
        });
      },
      error: (error) => this.toastError(resolveApiError(error))
    });
    this.teacherApi.getTeacherEinAbs().subscribe({
      next: (response) => {
        this.einAbs.set(response.einAbs);
        if (this.selectedEinAb()) {
          const refreshed = response.einAbs.find((item) => item.id === this.selectedEinAb()?.id) ?? null;
          this.selectedEinAb.set(refreshed);
        }
      },
      error: (error) => this.toastError(resolveApiError(error))
    });
    this.teacherApi.getTeacherIcalCandidates().subscribe({
      next: (response) => this.icalCandidates.set(response.candidates),
      error: () => this.icalCandidates.set([])
    });
  }

  saveTeacherProfile(): void {
    if (this.teacherProfileForm.invalid) {
      return;
    }
    this.profileSaveLoading.set(true);
    const formValue = this.teacherProfileForm.getRawValue();
    const updateTeacherMeRequest: UpdateTeacherMeRequest = {
      icalLink: formValue.icalLink?.trim() || undefined
    };
    this.teacherApi.updateTeacherMe({ updateTeacherMeRequest }).subscribe({
      next: (response: TeacherSelfResponse) => {
        this.teacher.set(response);
        this.teacherProfileForm.reset({ icalLink: response.icalLink ?? '' });
        this.profileSaveLoading.set(false);
      },
      error: (error) => {
        this.profileSaveLoading.set(false);
        this.toastError(resolveApiError(error));
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
      visitFairteiler: false,
      slotCount: 1
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
      publicLocation: candidate.location ?? '',
      whatToBring: '',
      visitFairteiler: false,
      slotCount: 1
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
      visitFairteiler: einab.visitFairteiler,
      slotCount: einab.slotCount
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
      publicLocation: formValue.publicLocation?.trim() || undefined,
      whatToBring: formValue.whatToBring?.trim() || undefined,
      visitFairteiler: formValue.visitFairteiler,
      slotCount: formValue.slotCount
    };

    const request$ = this.editingEinAb()
      ? this.teacherApi.updateTeacherEinAb({ einAbId: this.editingEinAb()!.id, upsertEinAbRequest })
      : this.teacherApi.createTeacherEinAb({ upsertEinAbRequest });

    request$.pipe(finalize(() => this.saveLoading.set(false))).subscribe({
      next: () => {
        this.einabDialogVisible = false;
        this.reload();
      },
      error: (error) => this.toastError(resolveApiError(error))
    });
  }

  confirmDelete(einab: TeacherEinAbResponse): void {
    this.confirmationService.confirm({
      message: this.i18n.t('confirm.deleteEinab'),
      accept: () => {
        this.teacherApi.deleteTeacherEinAb({ einAbId: einab.id }).subscribe({
          next: () => this.reload(),
          error: (error) => this.toastError(resolveApiError(error))
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
