import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { NotificationUnsubscribeRequest, PublicService } from '../../api';
import { resolveApiError } from '../../core/api-error';
import { I18nService } from '../../core/i18n.service';
import { CardModule } from 'primeng/card';
import { ProgressSpinnerModule } from 'primeng/progressspinner';

@Component({
  selector: 'app-unsubscribe-page',
  standalone: true,
  imports: [CommonModule, CardModule, ProgressSpinnerModule],
  templateUrl: './unsubscribe-page.component.html'
})
export class UnsubscribePageComponent implements OnInit {
  readonly i18n = inject(I18nService);

  protected readonly loading = signal(true);
  protected readonly title = signal(this.i18n.t('unsubscribe.title'));
  protected readonly message = signal('');

  private readonly route = inject(ActivatedRoute);
  private readonly publicApi = inject(PublicService);

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) {
      this.title.set(this.i18n.t('unsubscribe.title'));
      this.message.set(this.i18n.t('unsubscribe.missingToken'));
      this.loading.set(false);
      return;
    }

    const notificationUnsubscribeRequest: NotificationUnsubscribeRequest = { token };
    this.publicApi.unsubscribeNotifications({ notificationUnsubscribeRequest }).subscribe({
      next: () => {
        this.title.set(this.i18n.t('unsubscribe.successTitle'));
        this.message.set(this.i18n.t('unsubscribe.success'));
        this.loading.set(false);
      },
      error: (error) => {
        this.title.set(this.i18n.t('unsubscribe.title'));
        this.message.set(resolveApiError(error, this.i18n));
        this.loading.set(false);
      }
    });
  }
}
