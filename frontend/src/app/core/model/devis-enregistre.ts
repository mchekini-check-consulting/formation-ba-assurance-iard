// US-12 : devis enregistré, repris depuis « Mes devis » ou téléchargé en PDF.
import { DevisLogement } from './devis';
import { Formule, Franchise, OptionConseillee, ReponsesBesoins, formaterPrix } from './recommandation';

/** Le prix n'est pas envoyé : le backend le calcule lui-même. */
export interface DemandeDevis {
  reference: string;
  logement: DevisLogement;
  formule: Formule;
  options: OptionConseillee[];
  franchise: Franchise;
}

export interface DevisEnregistre {
  reference: string;
  formule: Formule;
  typeLogement: string;
  surface: number;
  adresse: string;
  codePostal: string;
  ville: string;
  prixMensuel: number;
  prixAnnuel: number;
  dateFinValidite: string;
  expire: boolean;
}

export interface RepriseDevis {
  reference: string;
  logement: DevisLogement;
  besoins: ReponsesBesoins;
  formule: Formule;
  options: OptionConseillee[];
  franchise: Franchise;
  prixMensuel: number;
  /** Rempli seulement si le devis avait expiré et que son prix a été recalculé. */
  ancienPrixMensuel: number | null;
  dateFinValidite: string;
}

export const MENTION_CONSERVATION =
  'Votre devis est conservé 3 ans après votre dernier contact. Vous pouvez le supprimer à tout moment depuis « Mes devis ».';

/** Date reçue au format 2026-12-05, affichée 05/12/2026. */
export function formaterDate(date: string): string {
  return date.split('-').reverse().join('/');
}

/** AC6 : le client est informé du recalcul d'un devis expiré. */
export function messageReprise(reprise: RepriseDevis): string {
  const nouveau = formaterPrix(reprise.prixMensuel);
  const fin = formaterDate(reprise.dateFinValidite);
  if (reprise.ancienPrixMensuel === null) {
    return `Votre devis est repris. Son prix est garanti jusqu'au ${fin}.`;
  }
  const ancien = formaterPrix(reprise.ancienPrixMensuel);
  const evolution =
    ancien === nouveau ? `il reste à ${nouveau} € par mois` : `il passe de ${ancien} € à ${nouveau} € par mois`;
  return `Votre devis avait expiré : son prix a été recalculé et ${evolution}, garanti jusqu'au ${fin}.`;
}
