import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { Droit, PAGE_ACCES_NON_AUTORISE, Rubrique } from '../model/droits';
import { ConnexionService } from '../service/connexion.service';
import { InactiviteService } from '../service/inactivite.service';

/**
 * US-04, AC3/AC5 : ouvre la page seulement si le profil a ce droit, relu auprès du serveur à chaque navigation.
 * `backOffice` : réservée en plus aux profils internes. Le serveur refait toujours le contrôle (RG1).
 */
export function droitGuard(rubrique: Rubrique, droit: Droit, backOffice = false): CanActivateFn {
  return (_route, etat) => {
    const router = inject(Router);
    const connexion = inject(ConnexionService);
    const inactivite = inject(InactiviteService);
    return connexion.sessionValide().pipe(
      map((valide) => {
        if (!valide) {
          return router.createUrlTree(['/connexion'], { queryParams: { retour: etat.url } });
        }
        if ((backOffice && !connexion.estInterne()) || !connexion.aLeDroit(rubrique, droit)) {
          return router.parseUrl(PAGE_ACCES_NON_AUTORISE);
        }
        inactivite.surveiller();
        return true;
      }),
    );
  };
}
