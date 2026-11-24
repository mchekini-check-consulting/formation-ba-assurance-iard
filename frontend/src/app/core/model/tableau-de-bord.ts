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
  numero: string | null;
  numeroContrat: string;
  type: string;
  dateSurvenance: string;
  declareLe: string | null;
}

const LIBELLES_SINISTRE: Record<string, string> = {
  DEGAT_EAUX: 'Dégât des eaux',
  INCENDIE: 'Incendie',
  VOL: 'Vol',
  BRIS_GLACE: 'Bris de glace',
  ACCIDENT_TIERS: 'Accident avec un tiers',
  ACCIDENT_SANS_TIERS: 'Accident sans tiers',
  CLIMATIQUE_CATNAT: 'Événement climatique ou catastrophe naturelle',
  AUTRE: 'Autre',
};

export function libelleSinistre(type: string): string {
  return LIBELLES_SINISTRE[type] ?? type;
}

/** Statut d'un sinistre affiché (les brouillons ne sont jamais renvoyés par le serveur). */
export const LIBELLE_SINISTRE_DECLARE = 'Déclaré';
