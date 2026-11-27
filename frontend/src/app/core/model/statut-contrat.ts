import { StatutContrat } from './contrat';

/** Les actions proposées sur un contrat (raccourcis de l'AC2 de l'US-18). */
export type ActionContrat =
  | 'detail'
  | 'signer'
  | 'modifierGaranties'
  /** US-23, AC5 : relire et accepter (ou abandonner) l'avenant en attente. */
  | 'relireAvenant'
  | 'regenerer'
  | 'genererFacture'
  | 'declarerSinistre';

/** Couleur d'accompagnement du badge : le texte du statut est toujours affiché (AC5, jamais la couleur seule). */
export type TonStatut = 'orange' | 'bleu' | 'vert' | 'rouge' | 'gris';

export interface DescriptionStatut {
  libelle: string;
  ton: TonStatut;
  message: string;
  actions: readonly ActionContrat[];
}

/**
 * US-18, RG3 : libellé, couleur, message et actions possibles de chaque statut. « Voir les factures » est rattaché à
 * l'action « Générer une facture » (interprétation à valider PO).
 * US-21, RG2 (recette) : « Régénérer » est possible pour tout contrat signé et en vigueur, sauf Suspendu : jamais
 * avant la signature, ni pour un contrat Résilié ou Renoncé (ses anciens documents restent téléchargeables). Elle
 * prime sur RG3 d'US-18 pour Impayé et Résiliation en cours (à valider PO).
 * Même règle côté serveur : RegenerationContratService.STATUTS_EXCLUS.
 */
export const STATUTS_CONTRAT: Record<StatutContrat, DescriptionStatut> = {
  EN_ATTENTE_DE_SIGNATURE: {
    libelle: 'En attente de signature',
    ton: 'orange',
    message: 'Votre contrat attend votre signature.',
    actions: ['detail', 'signer'],
  },
  EN_ATTENTE_EFFET: {
    libelle: "En attente d'effet",
    ton: 'bleu',
    message: 'Votre contrat est signé : il prendra effet à sa date d’effet.',
    // US-22 : facture possible dès la signature (décision BA, à valider PO).
    actions: ['detail', 'regenerer', 'genererFacture'],
  },
  ACTIF: {
    libelle: 'Actif',
    ton: 'vert',
    message: 'Votre contrat est actif.',
    actions: ['detail', 'modifierGaranties', 'regenerer', 'genererFacture', 'declarerSinistre'],
  },
  ACTIF_JUSTIFICATIFS_ATTENDUS: {
    libelle: 'Actif · justificatifs attendus',
    ton: 'bleu',
    message: 'Votre contrat est actif. Pensez à nous envoyer les justificatifs demandés.',
    // US-23, AC6 (décision BA, à valider PO) : garanties modifiables seulement pour un contrat Actif.
    actions: ['detail', 'regenerer', 'genererFacture', 'declarerSinistre'],
  },
  AVENANT_EN_ATTENTE: {
    libelle: 'Avenant en attente',
    ton: 'orange',
    // US-23, AC5 : couvert aux conditions actuelles ; accepter ou abandonner l'avenant.
    message:
      'Une modification de vos garanties attend votre réponse. Vous restez couvert aux conditions actuelles tant que vous ne l’avez pas acceptée.',
    actions: ['detail', 'relireAvenant', 'regenerer', 'genererFacture', 'declarerSinistre'],
  },
  IMPAYE: {
    libelle: 'Impayé',
    ton: 'rouge',
    message: 'Un paiement n’a pas abouti : régularisez votre situation pour garder votre couverture.',
    // US-24, AC1 (décision PO du 27/11/2026) : pas de déclaration de sinistre en ligne (assistance 24h/24).
    actions: ['detail', 'regenerer', 'genererFacture'],
  },
  SUSPENDU: {
    libelle: 'Suspendu',
    ton: 'rouge',
    message: 'Vos garanties sont suspendues jusqu’à la régularisation de vos paiements.',
    // US-24, AC1 (décision PO du 27/11/2026) : pas de déclaration de sinistre en ligne (assistance 24h/24).
    actions: ['detail', 'genererFacture'],
  },
  RESILIATION_EN_COURS: {
    libelle: 'Résiliation en cours',
    ton: 'gris',
    message: 'Votre contrat sera résilié à sa date de fin ; vous restez couvert jusque-là.',
    actions: ['detail', 'regenerer', 'genererFacture', 'declarerSinistre'],
  },
  RESILIE: {
    libelle: 'Résilié',
    ton: 'gris',
    message: 'Votre contrat est résilié. Vos anciens documents et factures restent consultables.',
    // US-24, AC1 (décision PO du 27/11/2026) : sinistre déclarable moins de 2 ans après la date de fin (le raccourci
    // le vérifie, comme le serveur, ReglesDeclaration).
    actions: ['detail', 'genererFacture', 'declarerSinistre'],
  },
  RENONCE: {
    libelle: 'Renoncé',
    ton: 'gris',
    message: 'Vous avez renoncé à ce contrat. Ses documents restent consultables.',
    // US-22 : factures des périodes antérieures à la renonciation (décision BA, à valider PO).
    actions: ['detail', 'genererFacture'],
  },
};

/** Un statut inconnu (évolution future du serveur) reste affiché en texte, sans action. */
export function decrireStatut(statut: string): DescriptionStatut {
  return STATUTS_CONTRAT[statut as StatutContrat] ?? { libelle: statut, ton: 'gris', message: '', actions: ['detail'] };
}
