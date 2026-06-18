import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, provideAppInitializer, provideBrowserGlobalErrorListeners, inject } from '@angular/core';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import Aura from '@primeuix/themes/aura';
import { ConfirmationService, MessageService } from 'primeng/api';
import { providePrimeNG } from 'primeng/config';

import { provideApi } from './api/provide-api';
import { authInterceptor } from './core/auth.interceptor';
import { I18nService } from './core/i18n.service';
import { routes } from './app.routes';
import { environment } from '../environments/environment';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideAnimations(),
    provideHttpClient(withInterceptors([authInterceptor])),
    providePrimeNG({
      theme: {
        preset: Aura
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
