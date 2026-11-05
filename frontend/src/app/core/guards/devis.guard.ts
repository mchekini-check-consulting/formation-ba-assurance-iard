import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { DevisService } from '../service/devis.service';

/** L'étape 2 réutilise le tarif de l'étape 1. */
export const logementRenseigneGuard: CanActivateFn = () =>
  inject(DevisService).tarifBase() === null ? inject(Router).parseUrl('/devis/logement') : true;

/** Le choix de formule suppose une recommandation enregistrée, à jour des dernières réponses. */
export const recommandationEnregistreeGuard: CanActivateFn = () =>
  inject(DevisService).reference() ? true : inject(Router).parseUrl('/devis/besoins');

/** L'étape 4 suppose une formule et une franchise choisies à l'étape 3. */
export const formuleChoisieGuard: CanActivateFn = () => {
  const devis = inject(DevisService);
  return devis.reference() && devis.franchiseChoisie() ? true : inject(Router).parseUrl('/devis/formule');
};
