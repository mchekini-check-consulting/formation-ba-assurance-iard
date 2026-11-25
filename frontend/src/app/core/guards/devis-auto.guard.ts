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
