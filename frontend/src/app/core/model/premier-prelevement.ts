// US-14 : même calcul que PremierPrelevementService côté backend, pour un affichage immédiat.
import { lireDate } from './informations';

export const JOURS_PRELEVEMENT = [5, 10, 15] as const;
export type JourPrelevement = (typeof JOURS_PRELEVEMENT)[number];

const DELAI_MAX_JOURS = 90;
const DELAI_ECHEANCIER_JOURS = 14;

function jourSeul(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate());
}

function ajouterJours(date: Date, jours: number): Date {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate() + jours);
}

export function dateDepuisSaisie(texte: string): Date | null {
  const date = lireDate(texte.trim());
  return date && new Date(date.annee, date.mois - 1, date.jour);
}

export function formaterDate(date: Date): string {
  return date.toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric' });
}

// RG1
export function erreurDateDebut(texte: string, aujourdhui: Date): string | null {
  if (!texte.trim()) return 'Indiquez la date de début de votre contrat.';
  const date = dateDepuisSaisie(texte);
  if (!date) return 'Saisissez une date valide au format JJ/MM/AAAA.';
  const jour = jourSeul(aujourdhui);
  if (date <= jour) return 'La date de début doit être au plus tôt demain.';
  return date > ajouterJours(jour, DELAI_MAX_JOURS) ? 'La date de début doit être dans les 90 prochains jours.' : null;
}

/**
 * RG3 et RG4 : le premier jour choisi qui soit à la fois à partir de la date de début
 * et au moins 14 jours après la signature (délai d'envoi de l'échéancier).
 */
export function premierPrelevement(dateDebut: Date, jour: JourPrelevement, signature: Date): Date {
  const echeancier = ajouterJours(jourSeul(signature), DELAI_ECHEANCIER_JOURS);
  const auPlusTot = dateDebut > echeancier ? dateDebut : echeancier;
  const candidate = new Date(auPlusTot.getFullYear(), auPlusTot.getMonth(), jour);
  return candidate < auPlusTot ? new Date(auPlusTot.getFullYear(), auPlusTot.getMonth() + 1, jour) : candidate;
}
