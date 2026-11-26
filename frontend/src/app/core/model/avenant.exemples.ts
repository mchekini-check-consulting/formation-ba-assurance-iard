import { Avenant, GarantiesContrat, ProjetAvenant } from './avenant';

/** US-23 : exemples des tests (Camille, AE-RECETTE-A03 Tous risques : 47,50 € → 48,50 € par mois). */
export const AVENANT_A03: Avenant = {
  numero: 'AV-2026-000009',
  rang: 1,
  statut: 'EN_ATTENTE',
  dateEffet: '2026-11-27',
  ancienneCotisationMensuelle: 47.5,
  nouvelleCotisationMensuelle: 48.5,
  ajouts: [{ code: 'ASSISTANCE_0KM', libelle: 'Assistance dès 0 km', prixMensuelTtc: 3.5 }],
  retraits: [{ code: 'PROTECTION_JURIDIQUE', libelle: 'Protection juridique', prixMensuelTtc: 2.5 }],
  accepteLe: null,
  documentId: null,
};

export const PROJET_A03: ProjetAvenant = {
  avenant: AVENANT_A03,
  ancienneCotisationAnnuelle: 570,
  nouvelleCotisationAnnuelle: 582,
  frequencePaiement: 'MENSUEL',
  premiereEcheanceConcernee: '2026-12-05',
  reductionCouverture: true,
  conseils: [
    'Votre couverture diminue : nous avons mis à jour notre conseil (article L521-4 du Code des assurances).',
    'Sans « Protection juridique », vous ne serez plus couvert pour vos frais de litige à partir du 27/11/2026.',
  ],
  documents: [{ id: 31, type: 'CONDITIONS_GENERALES', libelle: 'Conditions générales', version: 1, dateCreation: null }],
  ipidFormule: 'TR',
};

export const GARANTIES_A03: GarantiesContrat = {
  numero: 'AE-RECETTE-A03',
  produit: 'AUTO',
  formule: 'TR',
  statut: 'ACTIF',
  prenom: 'camille',
  cotisationMensuelle: 47.5,
  cotisationAnnuelle: 570,
  frequencePaiement: 'MENSUEL',
  garanties: [
    {
      code: 'RC',
      libelle: 'Responsabilité civile',
      prixMensuelTtc: 12,
      souscrite: true,
      obligatoire: true,
      modifiable: false,
      raison: 'La responsabilité civile est obligatoire : la loi impose d’assurer tout véhicule qui circule.',
    },
    { code: 'PROTECTION_JURIDIQUE', libelle: 'Protection juridique', prixMensuelTtc: 2.5, souscrite: true, obligatoire: false, modifiable: true, raison: null },
    { code: 'ASSISTANCE_0KM', libelle: 'Assistance dès 0 km', prixMensuelTtc: 3.5, souscrite: false, obligatoire: false, modifiable: true, raison: null },
  ],
  avenantEnAttente: null,
};
