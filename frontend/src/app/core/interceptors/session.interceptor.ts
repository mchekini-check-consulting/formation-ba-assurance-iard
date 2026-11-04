import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { apiBase } from '../config/api';
import { ConnexionService } from '../service/connexion.service';

/** Ajoute le jeton de session aux appels vers le backend, et à eux seuls. */
export const sessionInterceptor: HttpInterceptorFn = (requete, suivant) => {
  const jeton = inject(ConnexionService).jeton();
  if (!jeton || !requete.url.startsWith(`${apiBase('backend')}/`)) {
    return suivant(requete);
  }
  return suivant(requete.clone({ setHeaders: { Authorization: `Bearer ${jeton}` } }));
};
