import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { PublicService, TeacherSignupRequest } from '../../api';
import { resolveApiError } from '../../core/api-error';
import { BezirkContextService } from '../../core/bezirk-context.service';
import { I18nService } from '../../core/i18n.service';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { MessageService } from 'primeng/api';

@Component({
  selector: 'app-teacher-signup-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, CardModule, ButtonModule, InputTextModule],
  templateUrl: './teacher-signup-page.component.html'
})
export class TeacherSignupPageComponent {
  readonly i18n = inject(I18nService);
  protected readonly bezirkContext = inject(BezirkContextService);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    foodsharingId: ['', [Validators.required, Validators.pattern('^\\d+$')]]
  });

  private readonly publicApi = inject(PublicService);
  private readonly messageService = inject(MessageService);

  submit(): void {
    if (this.form.invalid) {
      return;
    }
    const teacherSignupRequest: TeacherSignupRequest = {
      foodsharingId: this.form.getRawValue().foodsharingId,
      language: this.i18n.apiLanguage()
    };
    this.publicApi.signupTeacher({
      bezirkSlug: this.bezirkContext.currentSlug(),
      teacherSignupRequest
    }).subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: this.i18n.t('teacherSignup.success') });
        this.form.reset();
      },
      error: (error) => this.messageService.add({ severity: 'error', summary: resolveApiError(error, this.i18n) })
    });
  }
}
