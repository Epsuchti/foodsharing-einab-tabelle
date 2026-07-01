import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { PublicService, TeacherSignupRequest } from '../../api';
import { resolveApiError } from '../../core/api-error';
import { I18nService } from '../../core/i18n.service';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { MessageService } from 'primeng/api';

@Component({
  selector: 'app-teacher-signup-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, CardModule, ButtonModule, InputTextModule],
  templateUrl: './teacher-signup-page.component.html'
})
export class TeacherSignupPageComponent {
  readonly i18n = inject(I18nService);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    name: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
    foodsharingId: ['', [Validators.required]],
    phoneNumber: ['', [Validators.required, Validators.minLength(3)]],
    icalLink: ['']
  });

  private readonly publicApi = inject(PublicService);
  private readonly messageService = inject(MessageService);

  submit(): void {
    if (this.form.invalid) {
      return;
    }
    const teacherSignupRequest: TeacherSignupRequest = {
      ...this.form.getRawValue(),
      language: this.i18n.apiLanguage()
    };
    this.publicApi.signupTeacher({ teacherSignupRequest }).subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: this.i18n.t('teacherSignup.success') });
        this.form.reset();
      },
      error: (error) => this.messageService.add({ severity: 'error', summary: resolveApiError(error, this.i18n) })
    });
  }
}
