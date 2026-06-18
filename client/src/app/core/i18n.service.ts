import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { EinAbCategory, SlotStatus, UserRole } from '../api';

type Language = 'de' | 'en' | 'gws';

@Injectable({ providedIn: 'root' })
export class I18nService {
  private readonly storageKey = 'foodsharing.language';
  private readonly http = inject(HttpClient);
  private readonly translations = signal<Record<string, string>>({});
  readonly language = signal<Language>(this.loadInitialLanguage());

  async initialize(): Promise<void> {
    await this.loadLanguage(this.language());
  }

  async setLanguage(language: Language): Promise<void> {
    this.language.set(language);
    localStorage.setItem(this.storageKey, language);
    await this.loadLanguage(language);
  }

  t(key: string): string {
    return this.translations()[key] ?? key;
  }

  categoryLabel(category: EinAbCategory | string | undefined): string {
    const key = category ?? '';
    const labels: Record<string, string> = {
      [EinAbCategory.Supermarket]: this.t('category.supermarket'),
      [EinAbCategory.Takeout]: this.t('category.takeout'),
      [EinAbCategory.Market]: this.t('category.market'),
      [EinAbCategory.Restaurant]: this.t('category.restaurant'),
      [EinAbCategory.FairteilerCleaning]: this.t('category.fairteilerCleaning')
    };
    return labels[key] ?? key;
  }

  statusLabel(status: SlotStatus | string | undefined): string {
    const key = status ?? '';
    const labels: Record<string, string> = {
      [SlotStatus.Available]: this.t('status.available'),
      [SlotStatus.Booked]: this.t('status.booked'),
      [SlotStatus.Done]: this.t('status.done'),
      [SlotStatus.Cancelled]: this.t('status.cancelled')
    };
    return labels[key] ?? key;
  }

  roleLabel(role: UserRole | string | undefined): string {
    const key = role ?? '';
    const labels: Record<string, string> = {
      [UserRole.User]: this.t('role.user'),
      [UserRole.Teacher]: this.t('role.teacher'),
      [UserRole.Admin]: this.t('role.admin')
    };
    return labels[key] ?? key;
  }

  private async loadLanguage(language: Language): Promise<void> {
    const translations = await firstValueFrom(this.http.get<Record<string, string>>(`/i18n/${language}.json`));
    this.translations.set(translations);
  }

  private loadInitialLanguage(): Language {
    const stored = localStorage.getItem(this.storageKey);
    return stored === 'de' || stored === 'en' || stored === 'gws' ? stored : 'gws';
  }
}
