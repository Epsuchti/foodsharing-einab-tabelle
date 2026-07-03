import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';

import { I18nService } from '../../core/i18n.service';
import { CardModule } from 'primeng/card';

@Component({
  selector: 'app-foodsharing-id-help-page',
  standalone: true,
  imports: [CommonModule, CardModule],
  templateUrl: './foodsharing-id-help-page.component.html',
  styleUrl: './foodsharing-id-help-page.component.scss'
})
export class FoodsharingIdHelpPageComponent {
  readonly i18n = inject(I18nService);
}
