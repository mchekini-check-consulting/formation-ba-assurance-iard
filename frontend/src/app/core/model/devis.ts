export type StatutLogement =
  | 'locataire'
  | 'colocation'
  | 'proprietaire-occupant'
  | 'proprietaire-non-occupant';

export type TypeLogement = 'appartement' | 'maison';
export type TypeResidence = 'principale' | 'secondaire';

export type Antecedent =
  | 'non-assure'
  | 'assure-moins-un-an'
  | 'assure-plus-un-an'
  | 'resiliation-autre'
  | 'resiliation-non-paiement';

export interface DevisLogement {
  statut: StatutLogement | null;
  residence: TypeResidence | null;
  typeLogement: TypeLogement | null;
  adresse: string;
  codePostal: string;
  ville: string;
  surface: string;
  nombrePieces: string;
  antecedent: Antecedent | null;
  etage: string;
  dependances: boolean | null;
  nombreColocataires: string;
  assureurActuel: string;
  nombreSinistres: string;
}

export function logementVide(): DevisLogement {
  return {
    statut: null,
    residence: null,
    typeLogement: null,
    adresse: '',
    codePostal: '',
    ville: '',
    surface: '',
    nombrePieces: '',
    antecedent: null,
    etage: '',
    dependances: null,
    nombreColocataires: '',
    assureurActuel: '',
    nombreSinistres: '',
  };
}

export function codePostalValide(cp: string): boolean {
  return /^\d{5}$/.test(cp);
}

export function surfaceValide(s: string): boolean {
  const n = Number(s);
  return s.trim() !== '' && Number.isInteger(n) && n >= 9 && n <= 1000;
}

// RG1 : 2 à 10 personnes dans la colocation (vous compris)
export function colocatairesValides(s: string): boolean {
  const n = Number(s);
  return s.trim() !== '' && Number.isInteger(n) && n >= 2 && n <= 10;
}

// PROVISOIRE : formule basée sur surface et type uniquement — à remplacer par un appel au backend devis
// Facteurs RG4 manquants : statut, pièces, zone code postal, résidence secondaire, étage, dépendances, sinistres
export function estimerTarif(surface: string, typeLogement: TypeLogement | null): number | null {
  if (!surfaceValide(surface) || !typeLogement) return null;
  const s = Number(surface);
  const base = 7.9;
  const facteur = typeLogement === 'maison' ? 0.03 : 0.025;
  return Math.round((base + s * facteur) * 10) / 10;
}
