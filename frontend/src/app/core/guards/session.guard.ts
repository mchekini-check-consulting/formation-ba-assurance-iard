import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { ConnexionService } from '../service/connexion.service';

/** AC6 : sans session, direction la connexion, en retenant la page demandée. */
export const sessionGuard: CanActivateFn = (_route, etat) => {
  const router = inject(Router);
  return inject(ConnexionService)
    .sessionValide()
    .pipe(map((valide) => valide || router.createUrlTree(['/connexion'], { queryParams: { retour: etat.url } })));
};

/** L'écran « Double authentification » suppose un e-mail et un mot de passe déjà validés (RG1). */
export const verificationGuard: CanActivateFn = () =>
  inject(ConnexionService).verification() ? true : inject(Router).parseUrl('/connexion');
