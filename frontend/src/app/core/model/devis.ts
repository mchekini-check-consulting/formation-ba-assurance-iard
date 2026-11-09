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

export type Etage = 'rdc' | 'intermediaire' | 'dernier';
export type TypeDependances = 'aucune' | 'jardin' | 'garage' | 'piscine' | 'plusieurs';

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
  resilieContrat: string;
  assuranceAntecedent: string;
  etage: string;
  dependances: TypeDependances | null;
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
    resilieContrat: '',
    assuranceAntecedent: '',
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

