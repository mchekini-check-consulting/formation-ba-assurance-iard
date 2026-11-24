/** T-05 / US-18 (RG3) : les 10 statuts qu'un contrat peut avoir en base. */
export type StatutContrat =
  | 'EN_ATTENTE_DE_SIGNATURE'
  | 'EN_ATTENTE_EFFET'
  | 'ACTIF'
  | 'ACTIF_JUSTIFICATIFS_ATTENDUS'
  | 'AVENANT_EN_ATTENTE'
  | 'IMPAYE'
  | 'SUSPENDU'
  | 'RESILIATION_EN_COURS'
  | 'RESILIE'
  | 'RENONCE';

export type Produit = 'HABITATION' | 'AUTO';

export interface DemandeContrat {
  referenceDevis: string;
  adresseLogement: string;
  codePostal: string;
  ville: string;
  typeLogement: string;
  surface: number;
  franchise: number;
  resiliationHamon: boolean;
}

/** US-15 : contrat habitation pendant la souscription (signature). */
export interface Contrat {
  numero: string;
  statut: StatutContrat;
  civilite: string | null;
  prenom: string;
  nom: string;
  telephone: string | null;
  adresseLogement: string;
  codePostal: string;
  ville: string;
  typeLogement: string;
  surface: number;
  formule: string;
  options: string[];
  franchise: number;
  cotisationMensuelle: number;
  cotisationAnnuelle: number;
  jourPrelevement: number;
  ibanFin: string;
  rum: string;
  datePremierPrelevement: string;
  dateEffet: string;
  resiliationHamon: boolean;
  dateCreation: string;
}

/** US-04 / US-18 : un contrat habitation ou auto dans « Mes contrats » et sur le tableau de bord. */
export interface ContratResume {
  /** Référence du devis de souscription (habitation du sprint 1) ; null pour l'auto. */
  referenceDevis: string | null;
  numero: string;
  produit: Produit;
  statut: StatutContrat;
  /** Logement (adresse) ou véhicule (marque, modèle, immatriculation), en une ligne. */
  bienAssure: string | null;
  cotisationMensuelle: number;
  dateEffet: string;
  /** Échéance annuelle ; null si le contrat se termine avant. */
  dateEcheance: string | null;
  dateFin: string | null;
}

/** US-18, AC3 : document téléchargeable par /documents-contrat/{id}. */
export interface DocumentContrat {
  id: number;
  type: string;
  libelle: string;
  version: number;
  dateCreation: string | null;
}

/** US-18, AC3 : fiche d'un contrat. L'IBAN n'est connu que masqué ; aucune donnée du conducteur. */
export interface FicheContrat {
  numero: string;
  referenceDevis: string | null;
  produit: Produit;
  statut: StatutContrat;
  formule: string;
  logement: { type: string | null; surface: number | null; adresse: string | null; codePostal: string | null; ville: string | null } | null;
  vehicule: { marque: string | null; modele: string | null; version: string | null; immatriculation: string | null } | null;
  franchise: number;
  garanties: { code: string; prixMensuelTtc: number | null }[];
  cotisationMensuelle: number;
  cotisationAnnuelle: number;
  dateEffet: string;
  dateEcheance: string | null;
  dateFin: string | null;
  paiement: { frequence: string | null; jourPrelevement: number | null; ibanMasque: string | null; rum: string | null };
  documents: DocumentContrat[];
}

export const LIBELLES_PRODUIT: Record<Produit, string> = {
  HABITATION: 'Habitation',
  AUTO: 'Auto',
};

/** Formules : habitation (sprint 1) et auto (référentiel du moteur, T-04). */
const LIBELLES_FORMULE: Record<string, string> = {
  essentielle: 'Essentielle',
  confort: 'Confort',
  serenite: 'Sérénité',
  TIERS: 'Tiers',
  INTER: 'Intermédiaire',
  TR: 'Tous risques',
};

/** Garanties et options : habitation (sprint 1) et auto (T-05). Un code inconnu est affiché tel quel. */
const LIBELLES_GARANTIE: Record<string, string> = {
  'objets-valeur': 'Objets de valeur',
  'assistance-depannage': 'Assistance dépannage',
  'protection-juridique': 'Protection juridique',
  RC: 'Responsabilité civile',
  BRIS_GLACE: 'Bris de glace',
  VOL: 'Vol',
  INCENDIE: 'Incendie',
  ASSISTANCE: 'Assistance',
  PROTECTION_JURIDIQUE: 'Protection juridique',
};

export function libelleFormule(code: string | null): string {
  return code ? (LIBELLES_FORMULE[code] ?? code) : '';
}

export function libelleGarantie(code: string): string {
  return LIBELLES_GARANTIE[code] ?? code;
}
