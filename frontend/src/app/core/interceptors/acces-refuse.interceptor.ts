import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { apiBase } from '../config/api';
import { PAGE_ACCES_NON_AUTORISE } from '../model/droits';

/** US-04, AC2 : un refus du serveur (403) affiche la page « Accès non autorisé », sans aucune donnée. */
export const accesRefuseInterceptor: HttpInterceptorFn = (requete, suivant) => {
  const router = inject(Router);
  return suivant(requete).pipe(
    catchError((erreur: unknown) => {
      if (
        erreur instanceof HttpErrorResponse &&
        erreur.status === 403 &&
        requete.url.startsWith(`${apiBase('assurance-iard-api')}/`)
      ) {
        void router.navigateByUrl(PAGE_ACCES_NON_AUTORISE);
      }
      return throwError(() => erreur);
    }),
  );
};
