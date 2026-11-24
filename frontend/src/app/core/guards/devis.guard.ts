import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { DevisService } from '../service/devis.service';

export const logementRenseigneGuard: CanActivateFn = () =>
  inject(DevisService).tarif() === null ? inject(Router).parseUrl('/devis/logement') : true;

/** Le choix de formule suppose une recommandation enregistrée, à jour des dernières réponses. */
export const recommandationEnregistreeGuard: CanActivateFn = () => {
  const devis = inject(DevisService);
  if (!devis.reference()) {
    devis.restaurerBrouillon();
  }
  return devis.reference() ? true : inject(Router).parseUrl('/devis/besoins');
};

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

/**
 * L'étape 6 suppose le mandat signé à l'étape 5. US-18 : depuis le tableau de bord, le raccourci « Signer » rouvre
 * l'étape 6 avec la référence du devis (?reference=) ; le serveur vérifie que le contrat existe et appartient au
 * client (un contrat n'existe qu'une fois le mandat signé).
 */
export const mandatSigneGuard: CanActivateFn = (route) => {
  const devis = inject(DevisService);
  if (devis.mandatSigne() || REFERENCE_DEVIS.test(route?.queryParamMap?.get('reference') ?? '')) {
    return true;
  }
  return inject(Router).parseUrl('/devis/paiement');
};

const REFERENCE_DEVIS = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
