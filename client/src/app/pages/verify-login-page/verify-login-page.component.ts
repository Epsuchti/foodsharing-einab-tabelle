import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { resolveApiError } from '../../core/api-error';
import { AuthFacadeService } from '../../core/auth-facade.service';
import { I18nService } from '../../core/i18n.service';
import { SessionService } from '../../core/session.service';
import { CardModule } from 'primeng/card';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { MessageService } from 'primeng/api';

@Component({
  selector: 'app-verify-login-page',
  standalone: true,
  imports: [CommonModule, CardModule, ProgressSpinnerModule],
  templateUrl: './verify-login-page.component.html'
})
export class VerifyLoginPageComponent implements OnInit {
  readonly i18n = inject(I18nService);
  protected readonly loading = signal(true);
  protected readonly error = signal('');

  private readonly route = inject(ActivatedRoute);
  private readonly authFacade = inject(AuthFacadeService);
  private readonly sessionService = inject(SessionService);
  private readonly router = inject(Router);
  private readonly messageService = inject(MessageService);

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) {
      this.loading.set(false);
      this.error.set(this.i18n.t('auth.missingToken'));
      return;
    }

    this.authFacade.verifyToken(token).subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: this.i18n.t('login.verified') });
        this.router.navigateByUrl(this.sessionService.primaryRoute());
      },
      error: (error) => {
        this.error.set(resolveApiError(error, this.i18n));
        this.loading.set(false);
      }
    });
  }
}
