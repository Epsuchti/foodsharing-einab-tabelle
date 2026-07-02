import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { RouterLink } from '@angular/router';

import { resolveApiError } from '../../core/api-error';
import { AuthFacadeService } from '../../core/auth-facade.service';
import { I18nService } from '../../core/i18n.service';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { MessageService } from 'primeng/api';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, CardModule, ButtonModule, InputTextModule, ProgressSpinnerModule],
  templateUrl: './login-page.component.html'
})
export class LoginPageComponent {
  readonly i18n = inject(I18nService);
  protected readonly loading = signal(false);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    foodsharingId: ['', [Validators.required]]
  });

  private readonly authFacade = inject(AuthFacadeService);
  private readonly messageService = inject(MessageService);

  submit(): void {
    if (this.form.invalid || this.loading()) {
      return;
    }
    this.loading.set(true);
    this.authFacade.requestLogin(this.form.getRawValue().foodsharingId).pipe(
      finalize(() => this.loading.set(false))
    ).subscribe({
      next: () => this.messageService.add({
        severity: 'success',
        summary: this.i18n.t('login.success')
      }),
      error: (error) => this.messageService.add({ severity: 'error', summary: resolveApiError(error, this.i18n) })
    });
  }
}
