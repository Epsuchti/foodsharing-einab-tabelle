import { Pipe, PipeTransform, inject } from '@angular/core';

import { I18nService } from './i18n.service';

@Pipe({
  name: 'zurichDateTime',
  standalone: true
})
export class ZurichDateTimePipe implements PipeTransform {
  private readonly i18n = inject(I18nService);

  transform(value: string | Date | null | undefined, variant: 'short' | 'medium' = 'medium'): string {
    if (!value) {
      return '';
    }

    const date = value instanceof Date ? value : new Date(value);
    if (Number.isNaN(date.getTime())) {
      return '';
    }

    const locale = this.i18n.language() === 'en' ? 'en-CH' : 'de-CH';
    const options: Intl.DateTimeFormatOptions = variant === 'short'
      ? { dateStyle: 'short', timeStyle: 'short', timeZone: 'Europe/Zurich' }
      : { dateStyle: 'medium', timeStyle: 'short', timeZone: 'Europe/Zurich' };

    return new Intl.DateTimeFormat(locale, options).format(date);
  }
}
