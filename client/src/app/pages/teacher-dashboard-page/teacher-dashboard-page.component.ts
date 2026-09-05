import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';

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
  protected readonly einAbsPage = signal<TeacherEinAbListResponse | null>(null);
  protected hidePastEinAbs = true;
  protected readonly selectedEinAb = signal<TeacherEinAbResponse | null>(null);
  protected readonly assignableTeachers = signal<TeacherAssignmentOption[]>([]);
  protected readonly assignTeacherDialogVisible = signal(false);
  protected readonly assignTeacherSlot = signal<SlotResponse | null>(null);
  protected readonly selectedTeacherId = signal<string | null>(null);
  protected readonly assignTeacherLoading = signal(false);
  protected readonly icalCandidates = signal<IcalCandidate[]>([]);
  protected readonly icalCandidatesPage = signal<IcalCandidateListResponse | null>(null);
  protected readonly bezirke = signal<BezirkResponse[]>([]);
  protected readonly bezirkSaveLoading = signal(false);
  protected readonly settingsSaveLoading = signal(false);
  protected readonly saveLoading = signal(false);
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

  protected readonly einabForm = inject(FormBuilder).nonNullable.group({
    category: [EinAbCategory.Supermarket, Validators.required],
    startDateTime: [new Date(), Validators.required],
    location: [''],
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
      this.einAbsPage.set(null);
      this.selectedEinAb.set(null);
      return;
    }
    this.teacherApi.getTeacherEinAbs({
      bezirkSlug: teacherBezirkSlug,
      page: this.einAbsPage()?.page ?? 0,
      size: this.pageSize,
      hidePast: this.hidePastEinAbs
    }).subscribe({
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

  onHidePastEinAbsChange(hidePast: boolean): void {
    this.hidePastEinAbs = hidePast;
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

  cancelSlotBooking(slot: SlotResponse): void {
    this.confirmationService.confirm({
      message: this.i18n.t('confirm.cancelTeacherBooking'),
      accept: () => {
        this.teacherApi.cancelTeacherSlotBooking({ bezirkSlug: this.teacher()!.bezirk!.slug, slotId: slot.id }).subscribe({
          next: () => this.reload(),
          error: (error) => this.toastError(resolveApiError(error, this.i18n))
        });
      }
    });
  }

  openAssignTeacher(slot: SlotResponse): void {
    if (slot.status !== SlotStatus.Booked || !slot.bookingUser) {
      return;
    }
    this.assignTeacherSlot.set(slot);
    this.selectedTeacherId.set(slot.teacherId);
    this.assignTeacherDialogVisible.set(true);
  }

  assignTeacherToSlot(): void {
    const slot = this.assignTeacherSlot();
    const teacherId = this.selectedTeacherId();
    const bezirkSlug = this.teacher()?.bezirk?.slug;
    if (!slot || !teacherId || !bezirkSlug || this.assignTeacherLoading()) {
      return;
    }
    this.assignTeacherLoading.set(true);
    this.teacherApi.assignTeacherToSlot({
      bezirkSlug,
      slotId: slot.id,
      assignTeacherToSlotRequest: { teacherId }
    }).subscribe({
      next: () => {
        this.assignTeacherLoading.set(false);
        this.assignTeacherDialogVisible.set(false);
        this.assignTeacherSlot.set(null);
        this.loadTeacherEinAbs(bezirkSlug);
        this.messageService.add({ severity: 'success', summary: this.i18n.t('teacher.assignSlotSuccess') });
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

  protected isOnline(category: EinAbCategory | undefined): boolean {
    return category === EinAbCategory.Online;
  }

  private configureOnlineFields(category: EinAbCategory): void {
    const isOnline = this.isOnline(category);
    const publicLocation = this.einabForm.controls.publicLocation;
    const onlineCallLink = this.einabForm.controls.onlineCallLink;
    publicLocation.setValidators(isOnline ? [] : [Validators.required]);
    onlineCallLink.setValidators(isOnline ? [Validators.required] : []);
    if (isOnline) {
      this.einabForm.patchValue({ location: '', publicLocation: '', privateInfo: '', publicInfo: '', visitFairteiler: false, slotCount: 1, minimumPickupCount: null }, { emitEvent: false });
    } else {
      this.einabForm.patchValue({ onlineCallLink: '' }, { emitEvent: false });
    }
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
