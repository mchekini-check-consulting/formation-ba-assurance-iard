import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { ConnexionService } from '../service/connexion.service';
import { InactiviteService } from '../service/inactivite.service';

/** AC6 de l'US-06 : sans session, direction la connexion, en retenant la page demandée. */
export const sessionGuard: CanActivateFn = (_route, etat) => {
  const router = inject(Router);
  const inactivite = inject(InactiviteService);
  return inject(ConnexionService)
    .sessionValide()
    .pipe(
      map((valide) => {
        if (!valide) {
          return router.createUrlTree(['/connexion'], { queryParams: { retour: etat.url } });
        }
        inactivite.surveiller();
        return true;
      }),
    );
};

/** L'écran « Double authentification » suppose un e-mail et un mot de passe déjà validés (RG1). */
export const verificationGuard: CanActivateFn = () =>
  inject(ConnexionService).verification() ? true : inject(Router).parseUrl('/connexion');
