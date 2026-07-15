import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { UserPermission } from '../api';
import { BezirkContextService } from '../core/bezirk-context.service';
import { I18nService } from '../core/i18n.service';
import { SessionService } from '../core/session.service';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ToastModule } from 'primeng/toast';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, ButtonModule, ToastModule, ConfirmDialogModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class App {
  readonly i18n = inject(I18nService);
  readonly sessionService = inject(SessionService);
  readonly bezirkContext = inject(BezirkContextService);
  readonly UserPermission = UserPermission;
  readonly mobileMenuOpen = signal(false);

  private readonly router = inject(Router);

  setLanguage(language: 'de' | 'en' | 'gws'): void {
    void this.i18n.setLanguage(language);
  }

  toggleMobileMenu(): void {
    this.mobileMenuOpen.update((current) => !current);
  }

  closeMobileMenu(): void {
    this.mobileMenuOpen.set(false);
  }

  switchBezirk(): void {
    this.closeMobileMenu();
    this.bezirkContext.selectedBezirk.set(null);
    void this.router.navigate(['/']);
  }
}
