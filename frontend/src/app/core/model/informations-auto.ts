// US-31 : règles de l'étape 4 auto, séparées de l'écran pour pouvoir les tester une par une.

import {
  Civilite,
  dateDepuisIso,
  dateVersIso,
  erreurDateNaissance,
  formaterSaisieDate,
  lireDate,
  normaliserTelephone,
} from './informations';

export type { Civilite };
export { dateDepuisIso, dateVersIso, erreurDateNaissance, formaterSaisieDate, lireDate, normaliserTelephone };

export interface SaisieInformationsAuto {
  civilite: Civilite | null;
  nom: string;
  prenom: string;
  dateNaissance: string;
  telephone: string;
  adresse: string;
  codePostal: string;
  ville: string;
}

export function saisieAutoVide(): SaisieInformationsAuto {
  return { civilite: null, nom: '', prenom: '', dateNaissance: '', telephone: '', adresse: '', codePostal: '', ville: '' };
}

/** B4 : seuls les numéros mobiles 06 et 07 sont acceptés. */
export function erreurTelephoneMobile(texte: string): string | null {
  if (!texte.trim()) return 'Indiquez votre numéro de téléphone mobile.';
  return /^0[67]\d{8}$/.test(normaliserTelephone(texte))
    ? null
    : 'Saisissez un numéro mobile français (06 ou 07, 10 chiffres).';
}

export function erreurAdresse(texte: string): string | null {
  return texte.trim().length >= 5 ? null : 'Indiquez votre adresse (au moins 5 caractères).';
}

export function erreurCodePostal(texte: string): string | null {
  return /^\d{5}$/.test(texte.trim()) ? null : 'Saisissez un code postal à 5 chiffres.';
}

export function erreurVille(texte: string): string | null {
  return texte.trim().length >= 2 ? null : 'Indiquez votre ville (au moins 2 caractères).';
}
