import { CommonModule, DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';

import {
  BookingDetailResponse,
  CreateSlotCommentRequest,
  EinAbCategory,
  IcalCandidate,
  SlotCommentResponse,
  SlotResponse,
  SlotStatus,
  TeacherEinAbResponse,
  TeacherResponse,
  TeacherService,
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
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TextareaModule } from 'primeng/textarea';
import { ConfirmationService, MessageService } from 'primeng/api';

@Component({
  selector: 'app-teacher-dashboard-page',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    DatePipe,
    CardModule,
    ButtonModule,
    CheckboxModule,
    ConfirmDialogModule,
    DatePickerModule,
    DialogModule,
    InputTextModule,
    SelectModule,
    TableModule,
    TagModule,
    TextareaModule
  ],
  templateUrl: './teacher-dashboard-page.component.html'
})
export class TeacherDashboardPageComponent implements OnInit {
  readonly i18n = inject(I18nService);

  protected readonly teacher = signal<TeacherResponse | null>(null);
  protected readonly einAbs = signal<TeacherEinAbResponse[]>([]);
  protected readonly selectedEinAb = signal<TeacherEinAbResponse | null>(null);
  protected readonly bookings = signal<BookingDetailResponse[]>([]);
  protected readonly comments = signal<SlotCommentResponse[]>([]);
  protected readonly icalCandidates = signal<IcalCandidate[]>([]);
  protected readonly saveLoading = signal(false);
  protected readonly categoryOptions = computed(() => Object.values(EinAbCategory).map((value) => ({ value, label: this.i18n.categoryLabel(value) })));
  protected readonly slotStatusOptions = computed(() => Object.values(SlotStatus).map((value) => ({ value, label: this.i18n.statusLabel(value) })));
  protected readonly slotCountOptions = [1, 2, 3].map((value) => ({ value, label: String(value) }));
  protected readonly slotStatusDraft: Record<string, SlotStatus> = {};

  protected einabDialogVisible = false;
  protected commentsVisible = false;
  protected currentCommentSlotId?: string;
  protected readonly editingEinAb = signal<TeacherEinAbResponse | null>(null);

  protected readonly einabForm = inject(FormBuilder).nonNullable.group({
    category: [EinAbCategory.Supermarket, Validators.required],
    startDateTime: [new Date(), Validators.required],
    location: [''],
    visitFairteiler: [false],
    slotCount: [1, Validators.required]
  });

  protected readonly commentForm = inject(FormBuilder).nonNullable.group({
    comment: ['', Validators.required]
  });

  private readonly teacherApi = inject(TeacherService);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.teacherApi.getTeacherMe().subscribe({
      next: (response) => this.teacher.set(response),
      error: (error) => this.toastError(resolveApiError(error))
    });
    this.teacherApi.getTeacherEinAbs().subscribe({
      next: (response) => {
        this.einAbs.set(response.einAbs);
        if (this.selectedEinAb()) {
          const refreshed = response.einAbs.find((item) => item.id === this.selectedEinAb()?.id) ?? null;
          this.selectedEinAb.set(refreshed);
        }
        response.einAbs.flatMap((einab) => einab.slots).forEach((slot) => this.slotStatusDraft[slot.id] = slot.status);
      },
      error: (error) => this.toastError(resolveApiError(error))
    });
    this.teacherApi.getTeacherBookings().subscribe({
      next: (response) => this.bookings.set(response.bookings),
      error: (error) => this.toastError(resolveApiError(error))
    });
    this.teacherApi.getTeacherIcalCandidates().subscribe({
      next: (response) => this.icalCandidates.set(response.candidates),
      error: () => this.icalCandidates.set([])
    });
  }

  selectEinAb(einab: TeacherEinAbResponse): void {
    this.selectedEinAb.set(einab);
  }

  openCreate(): void {
    this.editingEinAb.set(null);
    this.einabForm.reset({
      category: EinAbCategory.Supermarket,
      startDateTime: new Date(),
      location: '',
      visitFairteiler: false,
      slotCount: 1
    });
    this.einabDialogVisible = true;
  }

  openCreateFromCandidate(candidate: IcalCandidate): void {
    this.editingEinAb.set(null);
    this.einabForm.reset({
      category: EinAbCategory.Supermarket,
      startDateTime: new Date(candidate.startDateTime),
      location: candidate.location ?? '',
      visitFairteiler: false,
      slotCount: 1
    });
    this.einabDialogVisible = true;
  }

  openEdit(einab: TeacherEinAbResponse): void {
    this.editingEinAb.set(einab);
    this.einabForm.reset({
      category: einab.category,
      startDateTime: new Date(einab.startDateTime),
      location: einab.location ?? '',
      visitFairteiler: einab.visitFairteiler,
      slotCount: einab.slotCount
    });
    this.einabDialogVisible = true;
  }

  saveEinAb(): void {
    if (this.einabForm.invalid) {
      return;
    }
    this.saveLoading.set(true);
    const formValue = this.einabForm.getRawValue();
    const upsertEinAbRequest: UpsertEinAbRequest = {
      category: formValue.category,
      startDateTime: formValue.startDateTime.toISOString(),
      location: formValue.location?.trim() || undefined,
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

  saveSlotStatus(slot: SlotResponse): void {
    const status = this.slotStatusDraft[slot.id] ?? slot.status;
    this.teacherApi.updateTeacherSlotStatus({
      slotId: slot.id,
      updateSlotStatusRequest: { status }
    }).subscribe({
      next: () => this.reload(),
      error: (error) => this.toastError(resolveApiError(error))
    });
  }

  openComments(slot: SlotResponse): void {
    this.currentCommentSlotId = slot.id;
    this.commentsVisible = true;
    this.loadComments(slot.id);
  }

  addComment(): void {
    if (!this.currentCommentSlotId || this.commentForm.invalid) {
      return;
    }
    const createSlotCommentRequest: CreateSlotCommentRequest = this.commentForm.getRawValue();
    this.teacherApi.addTeacherSlotComment({
      slotId: this.currentCommentSlotId,
      createSlotCommentRequest
    }).subscribe({
      next: () => {
        this.commentForm.reset();
        this.loadComments(this.currentCommentSlotId!);
      },
      error: (error) => this.toastError(resolveApiError(error))
    });
  }

  private toastError(detail: string): void {
    this.messageService.add({ severity: 'error', summary: detail });
  }

  private loadComments(slotId: string): void {
    this.teacherApi.getTeacherSlotComments({ slotId }).subscribe({
      next: (response) => this.comments.set(response.comments),
      error: (error) => this.toastError(resolveApiError(error))
    });
  }
}
