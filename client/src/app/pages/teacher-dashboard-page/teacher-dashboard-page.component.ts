import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { catchError, finalize, forkJoin, map, of } from 'rxjs';

import {
  EinAbCategory,
  IcalCandidate,
  IcalCandidateListResponse,
  SlotStatus,
  SlotResponse,
  TeacherAssignmentOption,
  TeacherEinAbListResponse,
  TeacherEinAbResponse,
  TeacherResponse,
  TeacherSelfResponse,
  TeacherService,
  UpdateTeacherMeRequest,
  BezirkResponse,
  BookingCommentResponse,
  CreateBookingCommentRequest,
  UpsertEinAbRequest
} from '../../api';
import { resolveApiError } from '../../core/api-error';
import { BezirkContextService } from '../../core/bezirk-context.service';
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
    FormsModule,
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
  templateUrl: './teacher-dashboard-page.component.html',
  styleUrl: './teacher-dashboard-page.component.scss'
})
export class TeacherDashboardPageComponent implements OnInit {
  readonly i18n = inject(I18nService);
  readonly SlotStatus = SlotStatus;

  protected readonly teacher = signal<TeacherResponse | null>(null);
  protected readonly einAbs = signal<TeacherEinAbResponse[]>([]);
  protected readonly todoEinAbs = signal<TeacherEinAbResponse[]>([]);
  protected readonly einAbsPage = signal<TeacherEinAbListResponse | null>(null);
  protected showPastEinAbs = false;
  protected readonly assignableTeachers = signal<TeacherAssignmentOption[]>([]);
  protected readonly assignTeacherDialogVisible = signal(false);
  protected readonly assignTeacherEinAb = signal<TeacherEinAbResponse | null>(null);
  protected readonly selectedTeacherId = signal<string | null>(null);
  protected readonly assignTeacherLoading = signal(false);
  protected readonly icalCandidates = signal<IcalCandidate[]>([]);
  protected readonly icalCandidatesPage = signal<IcalCandidateListResponse | null>(null);
  protected readonly bezirke = signal<BezirkResponse[]>([]);
  protected readonly bezirkSaveLoading = signal(false);
  protected readonly settingsSaveLoading = signal(false);
  protected readonly saveLoading = signal(false);
  protected readonly commentsBySlotId = signal<Record<string, BookingCommentResponse[]>>({});
  protected readonly commentsLoaded = signal(false);
  protected readonly commentLoading = signal(false);
  protected readonly commentDialogVisible = signal(false);
  protected readonly commentTarget = signal<SlotResponse | null>(null);
  protected readonly categoryOptions = computed(() => Object.values(EinAbCategory).map((value) => ({ value, label: this.i18n.categoryLabel(value) })));
  protected readonly slotCountOptions = [1, 2, 3].map((value) => ({ value, label: String(value) }));
  protected readonly assignableTeacherOptions = computed(() => this.assignableTeachers().map((teacher) => ({
    label: teacher.phoneNumber ? `${teacher.name} (${teacher.phoneNumber})` : teacher.name,
    value: teacher.id
  })));
  protected readonly pageSize = 20;
  protected readonly minimumStartDate = new Date(new Date().setHours(0, 0, 0, 0));

  protected einabDialogVisible = false;
  protected readonly editingEinAb = signal<TeacherEinAbResponse | null>(null);
  protected readonly commentForm = inject(FormBuilder).nonNullable.group({
    comment: ['', [Validators.required, Validators.minLength(2)]]
  });

  protected readonly einabForm = inject(FormBuilder).nonNullable.group({
    category: [EinAbCategory.Supermarket, Validators.required],
    startDateTime: [new Date(), Validators.required],
    location: ['', Validators.required],
    publicLocation: ['', Validators.required],
    onlineCallLink: [''],
    privateInfo: [''],
    publicInfo: [''],
    visitFairteiler: [false],
    slotCount: [1, Validators.required],
    minimumPickupCount: [null as number | null]
  });

  protected readonly teacherSettingsForm = inject(FormBuilder).nonNullable.group({
    icalLink: ['']
  });

  protected readonly todoSlots = computed(() => {
    if (!this.commentsLoaded()) {
      return [];
    }
    return this.todoEinAbs().flatMap((einab) => this.isPast(einab.startDateTime)
      ? einab.slots
        .filter((slot) => slot.bookingUser && !this.commentsForSlot(slot.id).length)
        .map((slot) => ({ einab, slot }))
      : []);
  });

  private readonly teacherApi = inject(TeacherService);
  private readonly bezirkContext = inject(BezirkContextService);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);

  ngOnInit(): void {
    this.einabForm.controls.category.valueChanges.subscribe((category) => this.configureOnlineFields(category));
    this.reload();
  }

  reload(): void {
    this.teacherApi.getTeacherMe().subscribe({
      next: (response) => {
        this.teacher.set(response);
        this.teacherSettingsForm.reset({ icalLink: response.icalLink ?? '' });
        if (!response.bezirk) {
          this.bezirkContext.loadBezirke().subscribe((bezirke) => this.bezirke.set(bezirke));
        }
        this.loadTeacherEinAbs(response.bezirk?.slug);
        this.loadAssignableTeachers(response.bezirk?.slug);
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

  private loadTeacherEinAbs(teacherBezirkSlug?: string): void {
    if (!teacherBezirkSlug) {
      this.einAbs.set([]);
      this.todoEinAbs.set([]);
      this.einAbsPage.set(null);
      this.commentsBySlotId.set({});
      this.commentsLoaded.set(true);
      return;
    }
    this.commentsLoaded.set(false);
    forkJoin({
      visible: this.teacherApi.getTeacherEinAbs({
        bezirkSlug: teacherBezirkSlug,
        page: this.einAbsPage()?.page ?? 0,
        size: this.pageSize,
        pastOnly: this.showPastEinAbs
      }),
      todo: this.teacherApi.getTeacherEinAbs({
        bezirkSlug: teacherBezirkSlug,
        page: 0,
        size: 100,
        pastOnly: true
      }).pipe(catchError(() => of(null)))
    }).subscribe({
      next: ({ visible, todo }) => {
        const todoEinAbs = todo?.einAbs ?? [];
        this.einAbs.set(visible.einAbs);
        this.einAbsPage.set(visible);
        this.todoEinAbs.set(todoEinAbs);
        this.loadComments(teacherBezirkSlug, [...visible.einAbs, ...todoEinAbs].flatMap((einab) => einab.slots.filter((slot) => Boolean(slot.bookingUser)).map((slot) => slot.id)));
      },
      error: (error) => this.toastError(resolveApiError(error, this.i18n))
    });
  }

  private loadAssignableTeachers(teacherBezirkSlug?: string): void {
    if (!teacherBezirkSlug) {
      this.assignableTeachers.set([]);
      return;
    }
    this.teacherApi.getTeacherAssignableTeachers({ bezirkSlug: teacherBezirkSlug }).subscribe({
      next: (response) => this.assignableTeachers.set(response.teachers),
      error: (error) => this.toastError(resolveApiError(error, this.i18n))
    });
  }

  onEinAbsPageChange(event: { page?: number }): void {
    this.einAbsPage.update((current) => current ? { ...current, page: event.page ?? 0 } : current);
    this.reload();
  }

  onShowPastEinAbsChange(showPast: boolean): void {
    this.showPastEinAbs = showPast;
    this.einAbsPage.update((current) => current ? { ...current, page: 0 } : current);
    this.loadTeacherEinAbs(this.teacher()?.bezirk?.slug);
  }

  onIcalPageChange(event: { page?: number }): void {
    this.icalCandidatesPage.update((current) => current ? { ...current, page: event.page ?? 0 } : current);
    this.reload();
  }

  saveTeacherSettings(): void {
    this.settingsSaveLoading.set(true);
    const updateTeacherMeRequest: UpdateTeacherMeRequest = {
      language: this.i18n.apiLanguage(),
      icalLink: this.teacherSettingsForm.getRawValue().icalLink.trim() || undefined
    };
    this.teacherApi.updateTeacherMe({ updateTeacherMeRequest }).subscribe({
      next: (response: TeacherSelfResponse) => {
        this.teacher.set(response);
        this.icalCandidates.set(response.icalCandidates ?? []);
        this.icalCandidatesPage.update((current) => current ? { ...current, candidates: response.icalCandidates ?? [] } : current);
        this.teacherSettingsForm.reset({ icalLink: response.icalLink ?? '' });
        this.settingsSaveLoading.set(false);
      },
      error: (error) => {
        this.settingsSaveLoading.set(false);
        this.toastError(resolveApiError(error, this.i18n));
      }
    });
  }

  assignBezirk(bezirkSlug: string): void {
    if (!bezirkSlug || this.teacher()?.bezirk || this.bezirkSaveLoading()) {
      return;
    }
    this.bezirkSaveLoading.set(true);
    this.teacherApi.assignTeacherBezirk({ assignTeacherBezirkRequest: { bezirkSlug } }).subscribe({
      next: (teacher) => {
        this.teacher.set(teacher);
        this.bezirkSaveLoading.set(false);
        this.loadTeacherEinAbs(teacher.bezirk?.slug);
        this.loadAssignableTeachers(teacher.bezirk?.slug);
      },
      error: (error) => {
        this.bezirkSaveLoading.set(false);
        this.toastError(resolveApiError(error, this.i18n));
      }
    });
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
      onlineCallLink: '',
      privateInfo: '',
      publicInfo: '',
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
      onlineCallLink: '',
      privateInfo: '',
      publicInfo: '',
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
      onlineCallLink: einab.onlineCallLink ?? '',
      privateInfo: einab.privateInfo ?? '',
      publicInfo: einab.publicInfo ?? '',
      visitFairteiler: einab.visitFairteiler,
      slotCount: einab.slotCount,
      minimumPickupCount: einab.minimumPickupCount ?? null
    });
    this.configureOnlineFields(einab.category);
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
      publicLocation: this.isOnline(formValue.category) ? undefined : formValue.publicLocation.trim(),
      onlineCallLink: this.isOnline(formValue.category) ? formValue.onlineCallLink.trim() : undefined,
      privateInfo: formValue.privateInfo?.trim() || undefined,
      publicInfo: formValue.publicInfo?.trim() || undefined,
      visitFairteiler: this.isOnline(formValue.category) ? false : formValue.visitFairteiler,
      slotCount: formValue.slotCount,
      minimumPickupCount: this.isOnline(formValue.category) ? undefined : formValue.minimumPickupCount ?? undefined
    };

    const bezirkSlug = this.teacher()?.bezirk?.slug;
    if (!bezirkSlug) {
      this.toastError(this.i18n.t('teacher.unassignedHint'));
      this.saveLoading.set(false);
      return;
    }
    const request$ = this.editingEinAb()
      ? this.teacherApi.updateTeacherEinAb({ bezirkSlug, einAbId: this.editingEinAb()!.id, upsertEinAbRequest })
      : this.teacherApi.createTeacherEinAb({ bezirkSlug, upsertEinAbRequest });

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
        this.teacherApi.deleteTeacherEinAb({ bezirkSlug: this.teacher()!.bezirk!.slug, einAbId: einab.id }).subscribe({
          next: () => this.reload(),
          error: (error) => this.toastError(resolveApiError(error, this.i18n))
        });
      }
    });
  }

  cancelSlotBooking(slot: SlotResponse, startDateTime: string): void {
    this.confirmationService.confirm({
      message: this.i18n.t(this.isPast(startDateTime) ? 'confirm.markDidNotShowUp' : 'confirm.cancelTeacherBooking'),
      accept: () => {
        this.teacherApi.cancelTeacherSlotBooking({ bezirkSlug: this.teacher()!.bezirk!.slug, slotId: slot.id }).subscribe({
          next: () => this.reload(),
          error: (error) => this.toastError(resolveApiError(error, this.i18n))
        });
      }
    });
  }

  protected isPast(startDateTime: string): boolean {
    return new Date(startDateTime).getTime() < Date.now();
  }

  commentsForSlot(slotId: string): BookingCommentResponse[] {
    return this.commentsBySlotId()[slotId] ?? [];
  }

  openCommentDialog(slot: SlotResponse): void {
    if (!slot.bookingUser) return;
    this.commentTarget.set(slot);
    this.commentForm.reset({ comment: '' });
    this.commentDialogVisible.set(true);
  }

  saveComment(): void {
    const slot = this.commentTarget();
    const bezirkSlug = this.teacher()?.bezirk?.slug;
    const comment = this.commentForm.getRawValue().comment.trim();
    if (!slot?.bookingUser || !bezirkSlug || !comment || this.commentForm.invalid) return;

    this.commentLoading.set(true);
    const createBookingCommentRequest: CreateBookingCommentRequest = { comment };
    this.teacherApi.addTeacherSlotComment({ bezirkSlug, slotId: slot.id, createBookingCommentRequest }).subscribe({
      next: () => {
        this.commentDialogVisible.set(false);
        this.commentTarget.set(null);
        this.refreshComments(slot.id);
      },
      error: (error) => {
        this.commentLoading.set(false);
        this.toastError(resolveApiError(error, this.i18n));
      }
    });
  }

  openAssignTeacher(einab: TeacherEinAbResponse): void {
    this.assignTeacherEinAb.set(einab);
    this.selectedTeacherId.set(einab.teacher.id);
    this.assignTeacherDialogVisible.set(true);
  }

  assignTeacherToEinAb(): void {
    const einab = this.assignTeacherEinAb();
    const teacherId = this.selectedTeacherId();
    const bezirkSlug = this.teacher()?.bezirk?.slug;
    if (!einab || !teacherId || !bezirkSlug || this.assignTeacherLoading()) {
      return;
    }
    this.assignTeacherLoading.set(true);
    this.teacherApi.assignTeacherToEinAb({
      bezirkSlug,
      einAbId: einab.id,
      assignTeacherToEinAbRequest: { teacherId }
    }).subscribe({
      next: () => {
        this.assignTeacherLoading.set(false);
        this.assignTeacherDialogVisible.set(false);
        this.assignTeacherEinAb.set(null);
        this.loadTeacherEinAbs(bezirkSlug);
        this.messageService.add({ severity: 'success', summary: this.i18n.t('teacher.assignEinAbSuccess') });
      },
      error: (error) => {
        this.assignTeacherLoading.set(false);
        this.toastError(resolveApiError(error, this.i18n));
      }
    });
  }

  private toastError(detail: string): void {
    this.messageService.add({ severity: 'error', summary: this.i18n.t('common.error'), detail });
  }

  private loadComments(bezirkSlug: string, slotIds: string[]): void {
    const uniqueSlotIds = Array.from(new Set(slotIds));
    if (!uniqueSlotIds.length) {
      this.commentsBySlotId.set({});
      this.commentsLoaded.set(true);
      return;
    }
    forkJoin(uniqueSlotIds.map((slotId) => this.teacherApi.getTeacherSlotComments({ bezirkSlug, slotId }).pipe(
      map((response) => [slotId, response.comments] as const),
      catchError(() => of([slotId, []] as const))
    ))).subscribe((entries) => {
      this.commentsBySlotId.set(Object.fromEntries(entries));
      this.commentsLoaded.set(true);
    });
  }

  private refreshComments(slotId: string): void {
    const bezirkSlug = this.teacher()?.bezirk?.slug;
    if (!bezirkSlug) return;
    this.teacherApi.getTeacherSlotComments({ bezirkSlug, slotId }).subscribe({
      next: (response) => {
        this.commentsBySlotId.update((comments) => ({ ...comments, [slotId]: response.comments }));
        this.commentLoading.set(false);
      },
      error: (error) => {
        this.commentLoading.set(false);
        this.toastError(resolveApiError(error, this.i18n));
      }
    });
  }

  protected isOnline(category: EinAbCategory | undefined): boolean {
    return category === EinAbCategory.Online;
  }

  protected bookedSlotCount(einAb: TeacherEinAbResponse): number {
    return einAb.slots.filter((slot) => slot.status === SlotStatus.Booked).length;
  }

  private configureOnlineFields(category: EinAbCategory): void {
    const isOnline = this.isOnline(category);
    const location = this.einabForm.controls.location;
    const publicLocation = this.einabForm.controls.publicLocation;
    const onlineCallLink = this.einabForm.controls.onlineCallLink;
    location.setValidators(isOnline ? [] : [Validators.required]);
    publicLocation.setValidators(isOnline ? [] : [Validators.required]);
    onlineCallLink.setValidators(isOnline ? [Validators.required] : []);
    if (isOnline) {
      this.einabForm.patchValue({ location: '', publicLocation: '', privateInfo: '', publicInfo: '', visitFairteiler: false, slotCount: 1, minimumPickupCount: null }, { emitEvent: false });
    } else {
      this.einabForm.patchValue({ onlineCallLink: '' }, { emitEvent: false });
    }
    location.updateValueAndValidity({ emitEvent: false });
    publicLocation.updateValueAndValidity({ emitEvent: false });
    onlineCallLink.updateValueAndValidity({ emitEvent: false });
  }

  private requireActiveTeacher(): boolean {
    if (!this.teacher()?.bezirk) {
      this.toastError(this.i18n.t('teacher.unassignedHint'));
      return false;
    }
    if (this.teacher()?.active) {
      return true;
    }
    this.toastError(this.i18n.t('teacher.inactiveHint'));
    return false;
  }
}
