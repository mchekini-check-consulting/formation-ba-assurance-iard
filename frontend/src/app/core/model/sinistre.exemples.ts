import { DeclarationSinistre, TypesSinistre } from './sinistre';

/** US-24 : exemples de test, sur le modèle des données de recette de Camille (AE-RECETTE-A04, formule Tiers). */
export const TYPES_AUTO: TypesSinistre = {
  numeroContrat: 'AE-RECETTE-A04',
  produit: 'AUTO',
  numeroAssistance: '01 23 45 67 89',
  types: [
    {
      code: 'ACCIDENT_TIERS',
      libelle: 'Accident avec un tiers',
      aide: 'Un autre véhicule est impliqué',
      delaiPrevu: '5 jours ouvrés',
      demandeDateArrete: false,
      gestesUrgence: [],
      pieces: [{ code: 'CONSTAT_AMIABLE', libelle: 'Constat amiable (recto et verso)', obligatoire: true }],
    },
    {
      code: 'VOL',
      libelle: 'Vol du véhicule',
      aide: null,
      delaiPrevu: '2 jours ouvrés',
      demandeDateArrete: false,
      gestesUrgence: [],
      pieces: [],
    },
    {
      code: 'AUTRE',
      libelle: 'Autre / je ne sais pas',
      aide: null,
      delaiPrevu: '5 jours ouvrés',
      demandeDateArrete: false,
      gestesUrgence: [],
      pieces: [],
    },
  ],
};

/** Brouillon de vol sur un contrat Tiers (AC3 : non couvert), à l'étape 2. */
export const BROUILLON_VOL: DeclarationSinistre = {
  id: 42,
  numero: null,
  statut: 'BROUILLON',
  etape: 2,
  prenom: 'CAMILLE',
  numeroAssistance: '01 23 45 67 89',
  contrat: {
    numero: 'AE-RECETTE-A04',
    produit: 'AUTO',
    statut: 'ACTIF_JUSTIFICATIFS_ATTENDUS',
    formule: 'TIERS',
    bienAssure: 'Peugeot 208 · EF-004-GH',
    dateEffet: '2026-09-27',
    dateFin: null,
  },
  type: 'VOL',
  libelleType: 'Vol du véhicule',
  gestesUrgence: [],
  dateSurvenance: null,
  heureSurvenance: null,
  lieu: null,
  description: null,
  tiersImplique: null,
  tiers: null,
  blesses: null,
  nombreBlesses: null,
  declarant: null,
  declarantNom: null,
  declarantLien: null,
  details: {},
  dateArreteCatnat: null,
  explicationRetard: null,
  controles: { couvert: false, horsPeriodeContrat: false, horsDelai: false, delaiPrevu: '2 jours ouvrés', dateLimite: null },
  piecesAttendues: [
    { code: 'RECEPISSE_PLAINTE', libelle: 'Récépissé de dépôt de plainte', obligatoire: true },
    { code: 'FACTURES_EQUIPEMENTS', libelle: 'Factures d’équipements', obligatoire: false },
  ],
  piecesJointes: [],
  piecesManquantes: true,
  brouillonExpireLe: '2026-12-26T10:00:00Z',
  declareLe: null,
};

/** Le même brouillon, réponses de l'étape 2 remplies. */
export const BROUILLON_COMPLET: DeclarationSinistre = {
  ...BROUILLON_VOL,
  etape: 3,
  dateSurvenance: '2026-11-25',
  heureSurvenance: '08:30:00',
  lieu: 'Parking du travail',
  description: 'Véhicule disparu le matin',
  tiersImplique: false,
  blesses: false,
  declarant: 'SOUSCRIPTEUR',
  details: { 'Avez-vous déposé plainte ?': 'Oui' },
  controles: { ...BROUILLON_VOL.controles, couvert: false },
};
