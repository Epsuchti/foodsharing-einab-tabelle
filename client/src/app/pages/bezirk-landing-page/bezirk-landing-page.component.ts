import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { BezirkResponse } from '../../api';
import { resolveApiError } from '../../core/api-error';
import { BezirkContextService } from '../../core/bezirk-context.service';
import { I18nService } from '../../core/i18n.service';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { ProgressSpinnerModule } from 'primeng/progressspinner';

@Component({
  selector: 'app-bezirk-landing-page',
  standalone: true,
  imports: [CommonModule, RouterLink, ButtonModule, CardModule, ProgressSpinnerModule],
  templateUrl: './bezirk-landing-page.component.html',
  styleUrl: './bezirk-landing-page.component.scss'
})
export class BezirkLandingPageComponent implements OnInit {
  readonly i18n = inject(I18nService);
  protected readonly bezirke = signal<BezirkResponse[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal('');

  private readonly bezirkContext = inject(BezirkContextService);

  ngOnInit(): void {
    this.loadBezirke();
  }

  retry(): void {
    this.loadBezirke();
  }

  private loadBezirke(): void {
    this.loading.set(true);
    this.error.set('');
    this.bezirkContext.selectedBezirk.set(null);
    this.bezirkContext.loadBezirke().subscribe({
      next: (bezirke) => {
        this.bezirke.set(bezirke);
        this.loading.set(false);
      },
      error: (error) => {
        this.error.set(resolveApiError(error, this.i18n));
        this.loading.set(false);
      }
    });
  }
}
