import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

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
export class UnsubscribePageComponent implements OnInit {
  readonly i18n = inject(I18nService);
  protected message = '';

  private readonly route = inject(ActivatedRoute);
  private readonly publicApi = inject(PublicService);
  private readonly messageService = inject(MessageService);

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) {
      this.message = this.i18n.t('unsubscribe.missingToken');
      return;
    }
    const notificationUnsubscribeRequest: NotificationUnsubscribeRequest = { token };
    this.publicApi.unsubscribeNotifications({ notificationUnsubscribeRequest }).subscribe({
      next: () => this.message = this.i18n.t('unsubscribe.success'),
      error: (error) => {
        const detail = resolveApiError(error);
        this.message = detail;
        this.messageService.add({ severity: 'error', summary: detail });
      }
    });
  }
}
