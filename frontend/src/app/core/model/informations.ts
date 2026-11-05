// US-13 : règles de l'étape 4, séparées de l'écran pour pouvoir les tester une par une.

export type Civilite = 'madame' | 'monsieur';
export type CleZone = 'pieceIdentite' | 'domicile';

export interface OptionDocument {
  valeur: string;
  libelle: string;
}

// RG3
export const TYPES_PIECE_IDENTITE: readonly OptionDocument[] = [
  { valeur: 'carte-identite', libelle: "Carte d'identité" },
  { valeur: 'passeport', libelle: 'Passeport' },
  { valeur: 'titre-sejour', libelle: 'Titre de séjour' },
];

export const TYPES_JUSTIFICATIF_DOMICILE: readonly OptionDocument[] = [
  { valeur: 'electricite', libelle: "Facture d'électricité" },
  { valeur: 'gaz', libelle: 'Facture de gaz' },
  { valeur: 'internet', libelle: "Facture d'internet" },
  { valeur: 'quittance-loyer', libelle: 'Quittance de loyer' },
];

export interface ZoneDepot {
  type: string;
  fichier: File | null;
  /** Raison du refus du dernier fichier déposé (AC3). */
  erreur: string | null;
}

export interface SaisieInformations {
  civilite: Civilite | null;
  nom: string;
  prenom: string;
  dateNaissance: string;
  telephone: string;
  pieceIdentite: ZoneDepot;
  domicile: ZoneDepot;
  colocataires: string[];
}

/** Ce que le compte contient déjà : tout peut être vide (AC1). */
export interface IdentiteCompte {
  civilite: Civilite | null;
  nom: string | null;
  prenom: string | null;
  dateNaissance: string | null;
  telephone: string | null;
}

export function zoneVide(): ZoneDepot {
  return { type: '', fichier: null, erreur: null };
}

export function saisieVide(): SaisieInformations {
  return {
    civilite: null,
    nom: '',
    prenom: '',
    dateNaissance: '',
    telephone: '',
    pieceIdentite: zoneVide(),
    domicile: zoneVide(),
    colocataires: [],
  };
}

const AGE_MAJORITE = 18;
const ANNEE_MINIMALE = 1900;

export function lireDate(texte: string): { jour: number; mois: number; annee: number } | null {
  const morceaux = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(texte);
  if (!morceaux) return null;
  const [jour, mois, annee] = morceaux.slice(1).map(Number);
  const date = new Date(annee, mois - 1, jour);
  const existe = date.getFullYear() === annee && date.getMonth() === mois - 1 && date.getDate() === jour;
  return existe && annee >= ANNEE_MINIMALE ? { jour, mois, annee } : null;
}

// RG1 : majeur le jour même de ses 18 ans
function ageRevolu(naissance: { jour: number; mois: number; annee: number }, aujourdhui: Date): number {
  const moisActuel = aujourdhui.getMonth() + 1;
  const anniversairePasse =
    moisActuel > naissance.mois || (moisActuel === naissance.mois && aujourdhui.getDate() >= naissance.jour);
  return aujourdhui.getFullYear() - naissance.annee - (anniversairePasse ? 0 : 1);
}

export function erreurDateNaissance(texte: string, aujourdhui: Date): string | null {
  if (!texte.trim()) return 'Indiquez votre date de naissance.';
  const date = lireDate(texte.trim());
  if (!date) return 'Saisissez une date valide au format JJ/MM/AAAA.';
  return ageRevolu(date, aujourdhui) < AGE_MAJORITE ? 'Vous devez avoir au moins 18 ans pour souscrire.' : null;
}

/** Ajoute les barres obliques pendant la frappe : 31011990 devient 31/01/1990. */
export function formaterSaisieDate(texte: string): string {
  const chiffres = texte.replace(/\D/g, '').slice(0, 8);
  return [chiffres.slice(0, 2), chiffres.slice(2, 4), chiffres.slice(4)].filter(Boolean).join('/');
}

export function dateVersIso(texte: string): string {
  return texte.trim().split('/').reverse().join('-');
}

export function dateDepuisIso(iso: string): string {
  return iso.split('-').reverse().join('/');
}

// RG2 : les espaces sont acceptés à la saisie
export function normaliserTelephone(texte: string): string {
  return texte.replace(/\s/g, '');
}

export function erreurTelephone(texte: string): string | null {
  if (!texte.trim()) return 'Indiquez votre numéro de téléphone.';
  return /^0[1-9]\d{8}$/.test(normaliserTelephone(texte))
    ? null
    : 'Saisissez un numéro français à 10 chiffres, par exemple 06 12 34 56 78.';
}

// RG4
export const TAILLE_MAX_FICHIER = 5 * 1024 * 1024;
const FORMATS_ATTENDUS = 'Déposez un fichier PDF, JPG ou PNG de 5 Mo maximum.';

const SIGNATURES: readonly (readonly number[])[] = [
  [0x25, 0x50, 0x44, 0x46, 0x2d],
  [0xff, 0xd8, 0xff],
  [0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a],
];
const TAILLE_SIGNATURE = 8;

function extensionAcceptee(nom: string): boolean {
  return /\.(pdf|jpe?g|png)$/i.test(nom);
}

function contenuAccepte(octets: Uint8Array): boolean {
  return SIGNATURES.some((signature) => signature.every((octet, i) => octets[i] === octet));
}

/** Lit les premiers octets du fichier : c'est son contenu qui compte, pas seulement son extension. */
export async function erreurFichier(fichier: File): Promise<string | null> {
  if (fichier.size > TAILLE_MAX_FICHIER) return `Ce fichier dépasse 5 Mo. ${FORMATS_ATTENDUS}`;
  const debut = new Uint8Array(await fichier.slice(0, TAILLE_SIGNATURE).arrayBuffer());
  return extensionAcceptee(fichier.name) && contenuAccepte(debut)
    ? null
    : `Ce format n'est pas accepté. ${FORMATS_ATTENDUS}`;
}

/** AC4 : le nombre saisi à l'étape 1 compte le souscripteur. */
export function nombreAutresColocataires(nombreColocataires: string): number {
  return Math.max(Number(nombreColocataires) - 1, 0);
}
