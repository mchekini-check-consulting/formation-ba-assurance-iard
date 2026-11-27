import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { DevisAutoService } from '../service/devis-auto.service';

export const vehiculeRenseigneGuard: CanActivateFn = () => {
  const devisAuto = inject(DevisAutoService);
  return devisAuto.vehicule() !== null ? true : inject(Router).parseUrl('/devis-auto/vehicule');
};

export const achatRenseigneGuard: CanActivateFn = () => {
  const devisAuto = inject(DevisAutoService);
  if (devisAuto.vehicule() === null) return inject(Router).parseUrl('/devis-auto/vehicule');
  return devisAuto.achat() !== null ? true : inject(Router).parseUrl('/devis-auto/achat');
};

export const usageRenseigneGuard: CanActivateFn = () => {
  const devisAuto = inject(DevisAutoService);
  if (devisAuto.vehicule() === null) return inject(Router).parseUrl('/devis-auto/vehicule');
  if (devisAuto.achat() === null) return inject(Router).parseUrl('/devis-auto/achat');
  return devisAuto.usage() !== null ? true : inject(Router).parseUrl('/devis-auto/usage');
};

export const conducteurRenseigneGuard: CanActivateFn = () => {
  const devisAuto = inject(DevisAutoService);
  if (devisAuto.vehicule() === null) return inject(Router).parseUrl('/devis-auto/vehicule');
  if (devisAuto.achat() === null) return inject(Router).parseUrl('/devis-auto/achat');
  if (devisAuto.usage() === null) return inject(Router).parseUrl('/devis-auto/usage');
  return devisAuto.conducteur() !== null ? true : inject(Router).parseUrl('/devis-auto/conducteur');
};

export const besoinsRenseignesGuard: CanActivateFn = () => {
  const devisAuto = inject(DevisAutoService);
  if (devisAuto.vehicule() === null) return inject(Router).parseUrl('/devis-auto/vehicule');
  if (devisAuto.achat() === null) return inject(Router).parseUrl('/devis-auto/achat');
  if (devisAuto.usage() === null) return inject(Router).parseUrl('/devis-auto/usage');
  if (devisAuto.conducteur() === null) return inject(Router).parseUrl('/devis-auto/conducteur');
  return devisAuto.recommandation() !== null ? true : inject(Router).parseUrl('/devis-auto/besoins');
};

export const formuleChoisieAutoGuard: CanActivateFn = () => {
  const devisAuto = inject(DevisAutoService);
  if (devisAuto.vehicule() === null) return inject(Router).parseUrl('/devis-auto/vehicule');
  if (devisAuto.recommandation() === null) return inject(Router).parseUrl('/devis-auto/besoins');
  return devisAuto.formuleChoisie() !== null ? true : inject(Router).parseUrl('/devis-auto/formule');
};

export const souscripteurEnregistreAutoGuard: CanActivateFn = () => {
  const devisAuto = inject(DevisAutoService);
  return devisAuto.souscripteurEnregistre() ? true : inject(Router).parseUrl('/devis-auto/informations');
};

export const contratAutoGenereGuard: CanActivateFn = () => {
  const devisAuto = inject(DevisAutoService);
  return devisAuto.contratGenere() !== null ? true : inject(Router).parseUrl('/devis-auto/paiement');
};
