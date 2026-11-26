import { ContratResume, Produit } from './contrat';

/** US-18, AC1 : l'essentiel de l'espace client. Une rubrique non autorisée par le profil est vide et masquée. */
export interface TableauDeBord {
  prenom: string | null;
  nom: string | null;
  contrats: ContratResume[];
  devisVisibles: boolean;
  devisEnCours: DevisEnCours[];
  dernieresFactures: FactureResume[];
  sinistresVisibles: boolean;
  sinistres: SinistreResume[];
}

export interface DevisEnCours {
  produit: Produit;
  /** Adresse du logement ou véhicule. */
  description: string | null;
  formule: string;
  prixMensuel: number;
  dateFinValidite: string;
}

export interface FactureResume {
  numero: string;
  numeroContrat: string;
  periodeDebut: string;
  periodeFin: string;
  montantTtc: number;
  /** PDF téléchargeable ; null tant qu'il n'est pas disponible. */
  documentId: number | null;
}

export interface SinistreResume {
  /** US-24 : identifiant du dossier ou du brouillon (« Reprendre »). */
  id: number;
  numero: string | null;
  /** US-24, AC8 : DECLARE, ou BROUILLON pas encore expiré. */
  statut: 'DECLARE' | 'BROUILLON';
  numeroContrat: string;
  type: string;
  dateSurvenance: string | null;
  declareLe: string | null;
  /** US-24 : étape où reprendre un brouillon. */
  etape: number;
}

const LIBELLES_SINISTRE: Record<string, string> = {
  DEGAT_EAUX: 'Dégât des eaux',
  INCENDIE: 'Incendie',
  VOL: 'Vol',
  BRIS_GLACE: 'Bris de glace',
  ACCIDENT_TIERS: 'Accident avec un tiers',
  ACCIDENT_SANS_TIERS: 'Accident sans tiers',
  CLIMATIQUE_CATNAT: 'Événement climatique ou catastrophe naturelle',
  // US-24 : types habitation.
  VOL_CAMBRIOLAGE: 'Vol ou cambriolage',
  RC_TIERS: 'Dommage causé à un tiers',
  AUTRE: 'Autre / je ne sais pas',
};

export function libelleSinistre(type: string): string {
  return LIBELLES_SINISTRE[type] ?? type;
}

/** Statut d'un sinistre affiché. */
export const LIBELLE_SINISTRE_DECLARE = 'Déclaré';
/** US-24, AC8 : un brouillon pas encore expiré, à reprendre. */
export const LIBELLE_SINISTRE_BROUILLON = 'Brouillon';
