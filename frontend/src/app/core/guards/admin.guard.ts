import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { ConnexionService } from '../service/connexion.service';

/** Réserve la route aux administrateurs ; redirige les autres vers l'espace client. */
export const adminGuard: CanActivateFn = () => {
  const router = inject(Router);
  const connexionService = inject(ConnexionService);
  return connexionService.sessionValide().pipe(
    map((valide) => {
      if (!valide) {
        return router.createUrlTree(['/connexion']);
      }
      if (connexionService.profilClient() !== 'ADMINISTRATEUR') {
        return router.createUrlTree(['/espace-client']);
      }
      return true;
    }),
  );
};
