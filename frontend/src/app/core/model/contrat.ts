export type StatutContrat = 'EN_ATTENTE_DE_SIGNATURE' | 'SIGNE' | 'ACTIF' | 'ANNULE';

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

/** US-04 : un contrat dans « Mes contrats ». */
export interface ContratResume {
  referenceDevis: string;
  numero: string;
  statut: StatutContrat;
  adresseLogement: string;
  codePostal: string;
  ville: string;
  cotisationMensuelle: number;
  dateEffet: string;
}

export const LIBELLES_STATUT: Record<StatutContrat, string> = {
  EN_ATTENTE_DE_SIGNATURE: 'En attente de signature',
  SIGNE: 'Signé',
  ACTIF: 'Actif',
  ANNULE: 'Annulé',
};
