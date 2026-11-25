import { Produit, StatutContrat } from './contrat';

/** US-22 : statut d'une échéance (« Payée » si sa date est passée : simulation de prototype, à valider PO). */
export type StatutEcheance = 'PAYEE' | 'A_VENIR' | 'IMPAYEE';

/** US-22, AC2 : une facture se demande pour une échéance ou pour une année de cotisation. */
export type PeriodeFacture = 'ECHEANCE' | 'ANNEE';

export interface EcheanceFacture {
  date: string;
  montantTtc: number;
  statut: StatutEcheance;
  periodeDebut: string;
  periodeFin: string;
  /** Faux après la date de fin d'un contrat Résilié ou Renoncé. */
  facturable: boolean;
}

/** Année de cotisation : d'une date anniversaire du contrat à la suivante. */
export interface AnneeCotisation {
  debut: string;
  fin: string;
  montantTtc: number;
  facturable: boolean;
}

export interface FactureClient {
  numero: string;
  periodeDebut: string;
  periodeFin: string;
  montantHt: number;
  taxe: number;
  montantTtc: number;
  genereeLe: string;
  /** PDF téléchargeable ; null tant qu'il n'est pas disponible. */
  documentId: number | null;
}

/** AC1 : échéancier (du plus récent au plus ancien), années de cotisation et factures d'un contrat signé. */
export interface ContratFactures {
  numero: string;
  produit: Produit;
  statut: StatutContrat;
  bienAssure: string | null;
  echeances: EcheanceFacture[];
  annees: AnneeCotisation[];
  factures: FactureClient[];
}

export interface MesFactures {
  contrats: ContratFactures[];
  /** AC4 : aucune cotisation émise. */
  aucuneCotisation: boolean;
}

export interface FactureGeneree {
  facture: FactureClient;
  /** Faux si le PDF est encore en préparation. */
  disponible: boolean;
  /** Vrai si la facture existait déjà pour cette période (même numéro). */
  existante: boolean;
}

export const LIBELLES_STATUT_ECHEANCE: Record<StatutEcheance, string> = {
  PAYEE: 'Payée',
  A_VENIR: 'À venir',
  IMPAYEE: 'Impayée',
};

/** AC4 */
export const MESSAGE_AUCUNE_COTISATION = 'Vos factures apparaîtront ici dès la première cotisation.';
