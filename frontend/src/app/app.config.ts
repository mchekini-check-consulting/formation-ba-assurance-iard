import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { accesRefuseInterceptor } from './core/interceptors/acces-refuse.interceptor';
import { sessionInterceptor } from './core/interceptors/session.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    // HttpClient : appels aux backends (adresse via apiBase, src/app/api.ts)
    provideHttpClient(withFetch(), withInterceptors([sessionInterceptor, accesRefuseInterceptor])),
  ],
};
