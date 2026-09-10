import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, provideAppInitializer, provideBrowserGlobalErrorListeners, inject } from '@angular/core';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import Aura from '@primeuix/themes/aura';
import { definePreset } from '@primeuix/themes';
import { ConfirmationService, MessageService } from 'primeng/api';
import { providePrimeNG } from 'primeng/config';

import { provideApi } from './api/provide-api';
import { authInterceptor } from './core/auth.interceptor';
import { I18nService } from './core/i18n.service';
import { routes } from './app.routes';
import { environment } from '../environments/environment';

/**
 * One shared PrimeNG palette keeps buttons, form controls, overlays and data
 * components visually aligned with the application shell.  Page styles build
 * on these tokens instead of defining another colour system per view.
 */
const FoodsharingPreset = definePreset(Aura, {
  semantic: {
    primary: {
      50: '#effaf2',
      100: '#d8f3df',
      200: '#b4e7c2',
      300: '#82d59b',
      400: '#4cba70',
      500: '#25834a',
      600: '#1e713f',
      700: '#195d35',
      800: '#174b2d',
      900: '#123d25',
      950: '#082116'
    }
  }
});

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideAnimations(),
    provideHttpClient(withInterceptors([authInterceptor])),
    providePrimeNG({
      translation: {
        firstDayOfWeek: 1,
        dateFormat: 'dd.mm.yy'
      },
      theme: {
        preset: FoodsharingPreset,
        options: {
          darkModeSelector: 'none'
        }
      }
    }),
    provideApi(environment.apiBaseUrl),
    provideRouter(routes),
    MessageService,
    ConfirmationService,
    provideAppInitializer(() => {
      const i18n = inject(I18nService);
      return i18n.initialize();
    })
  ]
};
