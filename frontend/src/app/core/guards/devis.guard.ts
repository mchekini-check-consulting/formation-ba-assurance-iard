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

/** L'étape 5 suppose l'étape 4 enregistrée pour le devis en cours. */
export const informationsEnregistreesGuard: CanActivateFn = () => {
  const devis = inject(DevisService);
  const enregistre = devis.reference() !== null && devis.referenceDossier() === devis.reference();
  return enregistre ? true : inject(Router).parseUrl('/devis/informations');
};

/** L'étape 6 suppose le mandat signé à l'étape 5. */
export const mandatSigneGuard: CanActivateFn = () => {
  const devis = inject(DevisService);
  return devis.mandatSigne() ? true : inject(Router).parseUrl('/devis/paiement');
};
