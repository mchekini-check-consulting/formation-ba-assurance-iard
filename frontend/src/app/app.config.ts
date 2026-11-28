import { ApplicationConfig, LOCALE_ID, provideBrowserGlobalErrorListeners } from '@angular/core';
import { registerLocaleData } from '@angular/common';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import localeFr from '@angular/common/locales/fr';
import { routes } from './app.routes';
import { accesRefuseInterceptor } from './core/interceptors/acces-refuse.interceptor';
import { sessionInterceptor } from './core/interceptors/session.interceptor';

registerLocaleData(localeFr);

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    { provide: LOCALE_ID, useValue: 'fr-FR' },
    // HttpClient : appels aux backends (adresse via apiBase, src/app/api.ts)
    provideHttpClient(withFetch(), withInterceptors([sessionInterceptor, accesRefuseInterceptor])),
  ],
};
