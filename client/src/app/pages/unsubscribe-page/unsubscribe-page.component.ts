import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { catchError, map, of, startWith, tap } from 'rxjs';

import { NotificationUnsubscribeRequest, PublicService } from '../../api';
import { resolveApiError } from '../../core/api-error';
import { I18nService } from '../../core/i18n.service';
import { CardModule } from 'primeng/card';
import { MessageService } from 'primeng/api';

@Component({
  selector: 'app-unsubscribe-page',
  standalone: true,
  imports: [CommonModule, CardModule],
  templateUrl: './unsubscribe-page.component.html'
})
export class UnsubscribePageComponent {
  readonly i18n = inject(I18nService);

  private readonly route = inject(ActivatedRoute);
  private readonly publicApi = inject(PublicService);
  private readonly messageService = inject(MessageService);

  protected readonly viewModel = this.createViewModel();

  private createViewModel() {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) {
      return of({
        title: this.i18n.t('unsubscribe.title'),
        message: this.i18n.t('unsubscribe.missingToken')
      });
    }

    const notificationUnsubscribeRequest: NotificationUnsubscribeRequest = { token };
    return this.publicApi.unsubscribeNotifications({ notificationUnsubscribeRequest }).pipe(
      map(() => ({
        title: this.i18n.t('unsubscribe.successTitle'),
        message: this.i18n.t('unsubscribe.success')
      })),
      catchError((error) => {
        const detail = resolveApiError(error, this.i18n);
        return of({
          title: this.i18n.t('unsubscribe.title'),
          message: detail
        }).pipe(tap(() => {
          this.messageService.add({ severity: 'error', summary: detail });
        }));
      }),
      startWith({
        title: this.i18n.t('unsubscribe.title'),
        message: ''
      })
    );
  }
}
